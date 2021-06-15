package org.jetbrains.plugins.scala
package lang
package parameterInfo

import java.awt.Color

import com.intellij.codeInsight.hint.{HintUtil, ShowParameterInfoContext}
import com.intellij.lang.parameterInfo.{ParameterInfoHandlerWithTabActionSupport, ParameterInfoUIContext, *}
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.{PsiElement, PsiFile}
import com.intellij.testFramework.utils.parameterInfo.MockUpdateParameterInfoContext
import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestAdapter
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.junit.Assert.*

import scala.collection.mutable

abstract class ParameterInfoTestBase[Owner <: PsiElement] extends ScalaLightCodeInsightFixtureTestAdapter {

  override def getTestDataPath: String =
    s"${super.getTestDataPath}parameterInfo/"

  protected def createHandler: ParameterInfoHandlerWithTabActionSupport[Owner, Any, ? <: PsiElement]

  import ParameterInfoTestBase.*

  protected def configureFile(): PsiFile = {
    val filePath = s"${getTestName(false)}.scala"
    getFixture.configureByFile(filePath)
  }

  protected final def doTest(testUpdate: Boolean = true): Unit = {
    val file = configureFile()
    val offset = getFixture.getCaretOffset

    val context = new ShowParameterInfoContext(getEditor, getProject, file, offset, -1)
    val handler = createHandler

    val actual: Seq[String] = handleUI(handler, context)
    val expected: Seq[Seq[String]] = expectedSignatures(lastElement())

    assertNotNull(expected)
    assertTrue(s"expected signatures: ${expected.mkString(",")}, actual: ${actual.mkString(",")}", expected.contains(actual))

    if (testUpdate && actual.nonEmpty) {
      //todo test correct parameter index after moving caret
      val actualAfterUpdate = handleUpdateUI(handler, context)
      assertTrue(expected.contains(actualAfterUpdate))
    }
  }

  private def handleUI(handler: ParameterInfoHandler[Owner, Any],
                       context: CreateParameterInfoContext): Seq[String] = {
    val parameterOwner = handler.findElementForParameterInfo(context)
    val items = Option(context.getItemsToShow).getOrElse(Array.empty).toIndexedSeq
    uiStrings(items, handler, parameterOwner)
  }

  private def handleUpdateUI(handler: ParameterInfoHandler[Owner, Any],
                             context: CreateParameterInfoContext): Seq[String] = {
    val updatedContext = updateContext(context)
    val parameterOwner = handler.findElementForUpdatingParameterInfo(updatedContext)

    updatedContext.setParameterOwner(parameterOwner)
    handler.updateParameterInfo(parameterOwner, updatedContext)

    uiStrings(updatedContext.getObjectsToView.toIndexedSeq, handler, parameterOwner)
  }

  private def updateContext(context: CreateParameterInfoContext): UpdateParameterInfoContext = {
    val itemsToShow = context.getItemsToShow
    new MockUpdateParameterInfoContext(getEditor, getFile, itemsToShow) {
      private var items: Array[AnyRef] = itemsToShow

      override def getObjectsToView: Array[AnyRef] = items

      override def removeHint(): Unit = {
        items = Array.empty
      }
    }
  }

  private def lastElement() = {
    val file = getFile
    file.findElementAt(file.getTextLength - 1)
  }
}

object ParameterInfoTestBase {

  private def uiStrings[Owner <: PsiElement](items: Seq[AnyRef],
                                             handler: ParameterInfoHandler[Owner, Any],
                                             parameterOwner: Owner): Seq[String] = {
    val result = mutable.SortedSet.empty[String]
    items.foreach { item =>
      val uiContext = createInfoUIContext(parameterOwner) {
        result += _
      }
      handler.updateUI(item, uiContext)
    }

    result.toSeq.flatMap(normalize)
  }

  private[this] def createInfoUIContext[Owner <: PsiElement](parameterOwner: Owner)
                                                            (consume: String => Unit) = new ParameterInfoUIContext {
    override def getParameterOwner: PsiElement = parameterOwner

    override def setupUIComponentPresentation(text: String, highlightStartOffset: Int, highlightEndOffset: Int,
                                              isDisabled: Boolean, strikeout: Boolean, isDisabledBeforeHighlight: Boolean,
                                              background: Color): String = {
      consume(text)
      text
    }

    override def getDefaultParameterColor: Color = HintUtil.getInformationColor

    override def isUIComponentEnabled: Boolean = false

    override def getCurrentParameterIndex: Int = 0

    override def setUIComponentEnabled(enabled: Boolean): Unit = {}

    override def isSingleParameterInfo = false

    override def isSingleOverload = false

    override def setupRawUIComponentPresentation(htmlText: String): Unit = {}
  }

  private def expectedSignatures(lastElement: PsiElement): Seq[Seq[String]] = {
    val dropRight = lastElement.getNode.getElementType match {
      case ScalaTokenTypes.tLINE_COMMENT => 0
      case ScalaTokenTypes.tBLOCK_COMMENT | ScalaTokenTypes.tDOC_COMMENT => 2
    }

    val text = lastElement.getText
    text.substring(2, text.length - dropRight)
      .split("<--->")
      .map(normalize)
      .toIndexedSeq
  }

  private[this] def normalize(string: String) =
    StringUtil.convertLineSeparators(string)
      .split('\n')
      .map(_.trim)
      .filterNot(_.isEmpty)
      .toSeq
}

