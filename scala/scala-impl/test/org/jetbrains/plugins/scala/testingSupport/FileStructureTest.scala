package org.jetbrains.plugins.scala.testingSupport

import com.intellij.ide.structureView.newStructureView.StructureViewComponent
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.smartTree.{NodeProvider, TreeElement, TreeElementWrapper}
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.scala.debugger.ScalaDebuggerTestBase
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.structureView.ScalaStructureViewModel
import org.jetbrains.plugins.scala.lang.structureView.element.Test
import org.jetbrains.plugins.scala.testingSupport.test.structureView.TestNodeProvider
import org.jetbrains.plugins.scala.util.assertions.CollectionsAssertions.assertCollectionEquals
import org.junit.Assert.*

import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex

trait FileStructureTest {
  self: ScalaDebuggerTestBase =>

  trait FileStructureTreeAssert extends Function1[TreeElementWrapper, Unit]

  case class FileStructureNode(text: String, status: Option[Int]) {
    def nodeString: String = status.fold(text)(s => s"[$s] $text")
  }

  object FileStructureNode {
    private val StatusAndTextRegex: Regex = """\[(\d+)] (.*)""".r

    def parse(nodeString: String): FileStructureNode =
      nodeString match {
        case StatusAndTextRegex(status, text) =>
          FileStructureNode(text, Some(status.toInt))
        case _ =>
          FileStructureNode(nodeString, None)
      }
  }

  case class FileStructurePath(nodes: Seq[FileStructureNode]) {
    def pathString: String = nodes.map(_.nodeString).mkString(FileStructurePath.PartsSeparator)
  }

  object FileStructurePath {
    val PartsSeparator = " / "
    def p(s: String): FileStructurePath = parse(s)
    def parse(pathString: String): FileStructurePath = {
      val nodesStrings = pathString.split(PartsSeparator).toList
      val nodes = nodesStrings.map(FileStructureNode.parse)
      FileStructurePath(nodes)
    }
  }

  protected def runFileStructureViewTest(
    testClassName: String,
    status: Int,
    tests: String*
  ): Unit = {
    val structureViewRoot = buildFileStructureForClass(testClassName)
    tests.foreach(assertTestNodeInFileStructure(structureViewRoot, _, None, status))
  }

  protected def runFileStructureViewTest0(
    testClassName: String,
    assertTree: FileStructureTreeAssert
  ): Unit = {
    val structureViewRoot = buildFileStructureForClass(testClassName)
    assertTree(structureViewRoot)
  }

  protected def runFileStructureViewTest2(
    testClassName: String,
    status: Int
  )(tests: Seq[String]): Unit = {
    val structureViewRoot = buildFileStructureForClass(testClassName)
    tests.foreach(assertTestNodeInFileStructure(structureViewRoot, _, None, status))
  }

  protected def runFileStructureViewTest(
    testClassName: String,
    testName: String,
    parentTestName: Option[String],
    testStatus: Int = Test.NormalStatusId
  ): Unit = {
    val structureViewRoot = buildFileStructureForClass(testClassName)
    assertTestNodeInFileStructure(structureViewRoot, testName, parentTestName, testStatus)
  }

  private def buildFileStructureForClass(testClassName: String): TreeElementWrapper =
    buildFileStructure(testClassName + ".scala")

  private def buildFileStructure(fileName: String): TreeElementWrapper =
    inReadAction {
      val ioFile = new java.io.File(srcDir, fileName)
      val file = PsiManager.getInstance(getProject).findFile(getVirtualFile(ioFile))
      val treeViewModel = new ScalaStructureViewModel(file.asInstanceOf[ScalaFile]) {
        override def isEnabled(provider: NodeProvider[? <: TreeElement]): Boolean = provider.isInstanceOf[TestNodeProvider]
      }
      val wrapper = StructureViewComponent.createWrapper(getProject, treeViewModel.getRoot, treeViewModel)

      def initTree(wrapper: TreeElementWrapper): Unit = {
        wrapper.initChildren()
        wrapper.getChildren.asScala.foreach(node => initTree(node.asInstanceOf[TreeElementWrapper]))
      }

      initTree(wrapper)

      wrapper
    }

  protected def assertFileStructureTreePathsEqualsUnordered(
    root: TreeElementWrapper
  )(expectedPaths: Iterable[FileStructurePath]): Unit =
    AssertFileStructureTreePathsEqualsUnordered(expectedPaths)(root)

  protected def AssertFileStructureTreePathsEqualsUnordered(
    expectedPaths: Iterable[FileStructurePath]
  ): FileStructureTreeAssert = { root =>
    val actualPaths = allAvailablePaths(root)
    assertCollectionEquals(
      "Test tree paths don't match",
      expectedPaths.toSeq.map(_.pathString).sorted,
      actualPaths.map(_.pathString).sorted,
    )
  }

  private def assertTestNodeInFileStructure(
    root: TreeElementWrapper,
    expectedNodeName: String,
    expectedParentName: Option[String],
    expectedStatus: Int
  ): Unit = {

    def containsNodeWithName(currentNode: AbstractTreeNode[?], currentParentName: String): Boolean = {
      val treeElement = currentNode.getValue.asInstanceOf[TreeElement]
      val nodeName = treeElement.getPresentation.getPresentableText
      val matches = treeElement match {
        case test: Test =>
          expectedNodeName == nodeName &&
            expectedStatus == test.testStatus &&
            expectedParentName.forall(_ == currentParentName)
        case _ =>
          false
      }
      matches ||
        currentNode.getChildren.asScala.exists(containsNodeWithName(_, nodeName))
    }

    inReadAction {
      val containsNode = containsNodeWithName(root, "")
      if (!containsNode) {
        val parentStr = expectedParentName.fold("")(p => s" with parent '$p'")
        val allPaths = allAvailablePaths(root)
        val allPathsText = allPaths.map(_.pathString).mkString("\n")
        fail(
          s"""test node for test '$expectedNodeName'$parentStr was not found in file structure
             |available paths:
             |$allPathsText""".stripMargin
        )
      }
    }
  }

  private def allAvailablePaths(root: AbstractTreeNode[?]): Seq[FileStructurePath] = inReadAction {
    def inner(node: AbstractTreeNode[?], curPath: List[FileStructureNode]): Seq[Seq[FileStructureNode]] = {
      val children = node.getChildren
      val path = nodeInfo(node.getValue.asInstanceOf[TreeElement]) :: curPath
      if (children.isEmpty)
        path :: Nil
      else
        children.asScala.toSeq.flatMap(inner(_, path))
    }
    val pathsSeqs = inner(root, Nil)
    val paths = pathsSeqs.map(_.reverse).map(FileStructurePath.apply)
    paths.sortBy(_.pathString)
  }

  private def nodeInfo(node: TreeElement): FileStructureNode =
    node match {
      case test: Test =>
        val presentation = test.getPresentation
        val text = presentation.getPresentableText
        val status = presentation.asInstanceOf[Test].testStatus
        FileStructureNode(text, Some(status))
      case _ =>
        val presentation = node.getPresentation
        val text = presentation.getPresentableText
        FileStructureNode(text, None)
    }
}
