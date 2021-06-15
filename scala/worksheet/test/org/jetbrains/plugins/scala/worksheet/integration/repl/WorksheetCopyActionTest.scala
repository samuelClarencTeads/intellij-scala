package org.jetbrains.plugins.scala.worksheet.integration.repl

import com.intellij.openapi.editor.Editor
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.plugins.scala.WorksheetEvaluationTests
import org.jetbrains.plugins.scala.editor.DocumentExt
import org.jetbrains.plugins.scala.extensions.{StringExt, executeWriteActionCommand}
import org.jetbrains.plugins.scala.util.runners.*
import org.jetbrains.plugins.scala.worksheet.actions.topmenu.{CleanWorksheetAction, CopyWorksheetAction}
import org.junit.Assert.*
import org.junit.experimental.categories.Category

import scala.language.postfixOps

// no need in exhaustive check
@RunWithScalaVersions(Array(TestScalaVersion.Scala_2_13))
@Category(Array(classOf[WorksheetEvaluationTests]))
class WorksheetCopyActionTest extends WorksheetReplIntegrationBaseTest {

  private def prepareEditorForCopyAction(): Editor = doRenderTest(
    """println(1)
      |println(1 +
      |  2)
      |println("line 1\nline 2\nline 3")""".stripMargin,
    s"""1
       |3
       |
       |${foldStart}line 1
       |line 2
       |line 3$foldEnd""".stripMargin
  )

  def testCopyAction(): Unit = {
    val editor = prepareEditorForCopyAction()
    val result = CopyWorksheetAction.prepareCopiableText(editor, project)
    assertTrue(result.isDefined)
    assertEquals(
      """println(1)                                                                       //1
        |println(1 +                                                                      //3
        |  2)
        |println("line 1\nline 2\nline 3")                                                //line 1
        |                                                                                 //line 2
        |                                                                                 //line 3
        |""".stripMargin.withNormalizedSeparator,
      result.get
    )
  }

  def testCopyAction_AfterViewEditorWasAlreadyCleared(): Unit = {
    val editor = prepareEditorForCopyAction()

    CleanWorksheetAction.cleanAll(editor, project)
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

    val result = CopyWorksheetAction.prepareCopiableText(editor, project)
    assertTrue(result.isDefined)
    assertEquals(
      """println(1)
        |println(1 +
        |  2)
        |println("line 1\nline 2\nline 3")""".stripMargin.withNormalizedSeparator,
      result.get
    )
  }

  def testCopyAction_AfterRemovingLinesFromTheOriginalEditor(): Unit = {
    val editor = prepareEditorForCopyAction()

    executeWriteActionCommand() {
      val document = editor.getDocument
      document.deleteString(document.getLineStartOffset(1), document.getLineEndOffset(2) + 1)
      document.commit(project)
    }

    val result = CopyWorksheetAction.prepareCopiableText(editor, project)
    assertTrue(result.isDefined)
    // TODO: think how should copy result look like if left editor is being editted
  }
}
