package org.jetbrains.plugins.scala.lang.refactoring.mock

import java.util

import com.intellij.openapi.editor.{CaretState, LogicalPosition}

/**
 * Pavel Fatin
 */

class CaretModelMock(offset: Int, pos: LogicalPosition) extends CaretModelStub {

  override def setCaretsAndSelections(caretStates: util.List[? <: CaretState]): Unit = ()

  override def setCaretsAndSelections(caretStates: util.List[? <: CaretState], updateSystemSelection: Boolean): Unit = ()

  override def getOffset: Int = offset

  override def getLogicalPosition: LogicalPosition = pos
}