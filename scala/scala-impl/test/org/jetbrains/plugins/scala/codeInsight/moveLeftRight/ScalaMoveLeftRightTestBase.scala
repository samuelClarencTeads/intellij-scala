package org.jetbrains.plugins.scala.codeInsight.moveLeftRight

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.{EditorTestUtil, LightPlatformCodeInsightTestCase}
import org.jetbrains.plugins.scala.util.TestUtils

/**
  * @author Nikolay.Tropin
  */
abstract class ScalaMoveLeftRightTestBase extends LightPlatformCodeInsightTestCase {
  protected def doTestFromLeftToRight(leftMostPosition: String, rightPositions: String*): Unit = {
    doTest(moveLeft = true, leftMostPosition)
    doTest(false, leftMostPosition, rightPositions*)
  }

  protected def doTestFromRightToLeft(rightMostPosition: String, leftPositions: String*): Unit = {
    doTest(false, rightMostPosition)
    doTest(true, rightMostPosition, leftPositions*)
  }

  protected def checkMoveRightIsDisabled(fileText: String): Unit = {
    configureEditor(fileText)
    val actionId = IdeActions.MOVE_ELEMENT_RIGHT
    EditorTestUtil.executeAction(getEditor, actionId, false)
  }

  private def doTest(moveLeft: Boolean, before: String, after: String*): Unit = {
    var current: String = before
    for (next <- after) {
      doTestSingle(moveLeft, current, next)
      current = next
    }
    doTestSingle(moveLeft, current, current)
    if (after.isEmpty) return
    for (i <- after.length - 2 to 0 by -1) {
      val prev: String = after(i)
      doTestSingle(!moveLeft, current, prev)
      current = prev
    }
    doTestSingle(!moveLeft, current, before)
  }

  private def doTestSingle(moveLeft: Boolean, before: String, after: String): Unit = {
    configureEditor(before)
    val actionId =
      if (moveLeft) IdeActions.MOVE_ELEMENT_LEFT
      else IdeActions.MOVE_ELEMENT_RIGHT
    executeAction(actionId)
    checkResultByText(after)
  }

  private def configureEditor(fileText: String): Unit = {
    val fileName = s"${getTestName(false)}.scala"
    configureFromFileText(fileName, fileText)
  }

  override def setUp(): Unit = {
    super.setUp()
    TestUtils.disableTimerThread()
  }
}
