package org.jetbrains.plugins.scala
package base

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.impl.{DocumentImpl, TrailingSpacesStripper}
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.{VfsUtil, VirtualFile}
import com.intellij.psi.codeStyle.{CodeStyleSettings, CommonCodeStyleSettings}
import com.intellij.psi.{PsiDocumentManager, PsiFile}
import com.intellij.testFramework.fixtures.{JavaCodeInsightTestFixture, LightJavaCodeInsightFixtureTestCase}
import com.intellij.testFramework.{EditorTestUtil, LightPlatformTestCase, LightProjectDescriptor}
import junit.framework.TestCase
import org.jetbrains.plugins.scala.extensions.{inWriteCommandAction, invokeAndWait}
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings
import org.junit.Assert.{assertEquals, assertNotNull, fail}

import scala.jdk.CollectionConverters.*

/**
 * User: Dmitry Naydanov
 * Date: 3/5/12
 */
abstract class ScalaLightCodeInsightFixtureTestAdapter
  extends LightJavaCodeInsightFixtureTestCase
    with ScalaSdkOwner
    with TestFixtureProvider
    with FailableTest {

  import ScalaLightCodeInsightFixtureTestAdapter.*
  import libraryLoaders.*

  val CARET = EditorTestUtil.CARET_TAG
  val START = EditorTestUtil.SELECTION_START_TAG
  val END = EditorTestUtil.SELECTION_END_TAG

  protected def sourceRootPath: String = null

  override final def getFixture: JavaCodeInsightTestFixture = myFixture

  override def getTestDataPath: String = util.TestUtils.getTestDataPath + "/"

  protected def loadScalaLibrary: Boolean = true

  override protected def librariesLoaders: Seq[LibraryLoader] =
    ScalaSDKLoader() :: Option(sourceRootPath).map(SourcesLoader).toList

  override protected def getProjectDescriptor: LightProjectDescriptor = new ScalaLightProjectDescriptor(sharedProjectToken) {
    override def tuneModule(module: Module): Unit = setUpLibraries(module)
    override def getSdk: Sdk = SmartJDKLoader.getOrCreateJDK()
  }

  protected def sharedProjectToken: SharedTestProjectToken = SharedTestProjectToken.DoNotShare

  override def setUpLibraries(implicit module: Module): Unit = {
    Registry.get("ast.loading.filter").setValue(true, getTestRootDisposable)
    if (loadScalaLibrary) {
      getFixture.allowTreeAccessForAllFiles()
      super.setUpLibraries(module)
    }
  }

  override protected def tearDown(): Unit = {
    disposeLibraries(getModule)
    super.tearDown()
  }

  protected def configureFromFileText(fileText: String): PsiFile =
    configureFromFileText(fileText, ScalaFileType.INSTANCE)

  protected def configureFromFileText(fileText: String, fileType: FileType): PsiFile = {
    val file = getFixture.configureByText(fileType, normalize(fileText))
    assertNotNull(file)
    file
  }

  protected def configureFromFileText(fileText: String, fileType: String): PsiFile = {
    val file = getFixture.configureByText("Test." + fileType, normalize(fileText))
    assertNotNull(file)
    file
  }

  protected def getEditorOffset: Int = getEditor.getCaretModel.getOffset

  protected def checkTextHasNoErrors(text: String): Unit = {
    getFixture.configureByText(
      ScalaFileType.INSTANCE,
      text
    )
    CodeFoldingManager.getInstance(getProject).buildInitialFoldings(getEditor)

    if (shouldPass) {
      testHighlighting(getFile.getVirtualFile)
    } else {
      try {
        testHighlighting(getFile.getVirtualFile)
      } catch {
        case _: AssertionError => return
      }
      failingTestPassed()
    }
  }

  protected def checkHasErrorAroundCaret(text: String): Unit = {
    val normalizedText = normalize(text)
    myFixture.configureByText("dummy.scala", normalizedText)
    val caretIndex = normalizedText.indexOf(CARET)

    def isAroundCaret(info: HighlightInfo) = caretIndex == -1 || new TextRange(info.getStartOffset, info.getEndOffset).contains(caretIndex)
    val infos = myFixture.doHighlighting().asScala

    val warnings = infos.filter(i => StringUtil.isNotEmpty(i.getDescription) && isAroundCaret(i))

    if (shouldPass) {
      assert(warnings.nonEmpty, "No highlightings found")
    } else if (warnings.nonEmpty) {
      failingTestPassed()
    }
  }

  protected def checkCaretOffsets(
    expectedCarets: Seq[Int],
    stripTrailingSpaces: Boolean
  ): Unit = {
    checkCaretOffsets(expectedCarets, getEditor.getDocument.getText, stripTrailingSpaces)
  }

  protected def checkCaretOffsets(
    expectedCarets: Seq[Int],
    expectedText: String,
    stripTrailingSpaces: Boolean
  ): Unit = {
    val document = myFixture.getDocument(myFixture.getFile).asInstanceOf[DocumentImpl]
    if (stripTrailingSpaces) {
      TrailingSpacesStripper.strip(document, false, true)
    }

    checkCaretOffsets(
      expectedCarets,
      this.allCaretOffsets,
      expectedText,
      document.getText,
      stripTrailingSpaces
    )
  }

  private def doStripTrailingSpaces(text: String): String =
    text.replaceAll(" +\n", "\n")

  protected def checkCaretOffsets(
    expectedCarets: Seq[Int],
    actualCarets: Seq[Int],
    expectedText: String,
    actualText: String,
    stripTrailingSpaces: Boolean
  ): Unit = {
    if (expectedCarets.nonEmpty) {
      val expected0 = patchTextWithCarets(expectedText, expectedCarets)
      val expected= if (stripTrailingSpaces) doStripTrailingSpaces(expected0) else expected0
      val actual = patchTextWithCarets(actualText, actualCarets)
      assertEquals(expected, actual)
    }
  }

  protected def patchTextWithCarets(text: String, caretOffsets: Seq[Int]): String =
    caretOffsets
      .sorted(Ordering.Int.reverse)
      .foldLeft(text)(_.patch(_, "<caret>", 0))

  protected def failingTestPassed(): Unit = throw new RuntimeException(failingPassed)

  protected def getCurrentCodeStyleSettings: CodeStyleSettings = CodeStyle.getSettings(getProject)

  protected def getCommonSettings: CommonCodeStyleSettings = getCurrentCodeStyleSettings.getCommonSettings(ScalaLanguage.INSTANCE)

  protected def getScalaSettings: ScalaCodeStyleSettings = getCurrentCodeStyleSettings.getCustomSettings(classOf[ScalaCodeStyleSettings])

  private def testHighlighting(virtualFile: VirtualFile): Unit = getFixture.testHighlighting(
    false,
    false,
    false,
    virtualFile
  )

  protected def changePsiAt(offset: Int): Unit = {
    val settings = ScalaApplicationSettings.getInstance()
    val oldAutoBraceSettings = settings.HANDLE_BLOCK_BRACES_INSERTION_AUTOMATICALLY
    settings.HANDLE_BLOCK_BRACES_INSERTION_AUTOMATICALLY = false
    try {
      typeAndRemoveChar(offset, 'a')
    } finally settings.HANDLE_BLOCK_BRACES_INSERTION_AUTOMATICALLY = oldAutoBraceSettings
  }

  protected def typeAndRemoveChar(offset: Int, charToTypeAndRemove: Char): Unit = invokeAndWait {
    getEditor.getCaretModel.moveToOffset(offset)
    myFixture.`type`(charToTypeAndRemove)
    commitDocumentInEditor()
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_BACKSPACE)
    commitDocumentInEditor()
  }

  protected def insertAtOffset(offset: Int, text: String): Unit = invokeAndWait {
    inWriteCommandAction {
      getEditor.getDocument.insertString(offset, text)
      commitDocumentInEditor()
    }(getProject)
  }

  protected final def commitDocumentInEditor(): Unit =
    PsiDocumentManager.getInstance(getProject)
      .commitDocument(getEditor.getDocument)
}

object ScalaLightCodeInsightFixtureTestAdapter {

  def normalize(text: String, trimContent: Boolean = true): String = {
    val result = text.stripMargin.replace("\r", "")
    if (trimContent) result.trim else result
  }

  def findCaretOffset(text: String, stripTrailingSpaces: Boolean): (String, Int) = {
    val (textActual, caretOffsets) = findCaretOffsets(text, stripTrailingSpaces)
    caretOffsets match {
      case Seq(caretIdx) => (textActual, caretIdx)
      case Seq()         => (textActual, -1)
      case _             => fail(s"single caret expected but found: ${caretOffsets.size}").asInstanceOf[Nothing]
    }
  }

  def findCaretOffsets(text: String, trimText: Boolean): (String, Seq[Int]) = {
    import EditorTestUtil.CARET_TAG

    val textNormalized = normalize(text, trimText)

    def caretIndex(offset: Int) = textNormalized.indexOf(CARET_TAG, offset)

    @scala.annotation.tailrec
    def collectCaretIndices(idx: Int)(indices: Seq[Int]): Seq[Int] =
      if (idx < 0) indices else {
        val nextIdx = caretIndex(idx + 1)
        collectCaretIndices(nextIdx)(indices :+ idx)
      }

    val caretIndices = collectCaretIndices(caretIndex(0))(Seq[Int]())
    val caretIndicesNormalized = caretIndices.zipWithIndex.map { case (caretIdx, idx) => caretIdx - idx * CARET_TAG.length }
    (
      textNormalized.replace(CARET_TAG, ""),
      caretIndicesNormalized
    )
  }

  implicit class Ext(private val adapter: ScalaLightCodeInsightFixtureTestAdapter) extends AnyVal {

    def configureJavaFile(fileText: String,
                          className: String,
                          packageName: String = null): Unit = inWriteAction {
      val root = LightPlatformTestCase.getSourceRoot match {
        case sourceRoot if packageName == null => sourceRoot
        case sourceRoot => sourceRoot.createChildDirectory(null, packageName)
      }

      val file = root.createChildData(null, className + ".java")
      VfsUtil.saveText(file, normalize(fileText))
    }

    def allCaretOffsets: Seq[Int] =
      adapter.getFixture
        .getEditor
        .getCaretModel
        .getAllCarets.asScala
        .iterator
        .map(_.getOffset)
        .toSeq
  }
}
