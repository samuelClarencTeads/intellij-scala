package org.jetbrains.plugins.scala
package codeInsight
package generation
package actions

import com.intellij.lang.LanguageCodeInsightActionHandler
import org.junit.Assert.*

/**
 * Nikolay.Tropin
 * 8/23/13
 */
abstract class ScalaGenerateTestBase extends base.ScalaLightCodeInsightFixtureTestAdapter {

  import base.ScalaLightCodeInsightFixtureTestAdapter.*

  protected val handler: LanguageCodeInsightActionHandler

  protected final def performTest(text: String, expectedText: String,
                                  checkAvailability: Boolean = false, checkCaretOffset: Boolean = false): Unit = {
    val stripTrailingSpaces = true
    configureByText(text, stripTrailingSpaces)

    if (checkAvailability) {
      assertTrue("Generate action is not available", handlerIsValid)
    }

    extensions.executeWriteActionCommand("Generate Action Test") {
      handler.invoke(getProject, getEditor, getFile)
    }(getProject)

    val (expected, expectedOffset) = findCaretOffset(expectedText, stripTrailingSpaces)
    if (checkCaretOffset) {
      assertEquals("Wrong caret offset", expectedOffset, getEditorOffset)
    }
    getFixture.checkResult(expected, stripTrailingSpaces)
  }

  protected final def checkIsNotAvailable(text: String): Unit = {
    configureByText(text, stripTrailingSpaces = true)
    assertFalse("Generate action is available", handlerIsValid)
  }

  private def configureByText(text: String, stripTrailingSpaces: Boolean): Unit = {
    val (normalizedText, offset) = findCaretOffset(text, stripTrailingSpaces)

    getFixture.configureByText("dummy.scala", normalizedText)
    getEditor.getCaretModel.moveToOffset(offset)
  }

  private def handlerIsValid: Boolean = handler.isValidFor(getEditor, getFile)
}
