package org.jetbrains.plugins.scala.project.settings

import com.intellij.ui.SimpleListCellRenderer
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.project.IncrementalityType
import javax.swing.*

class IncrementalityTypeRenderer extends SimpleListCellRenderer[IncrementalityType] {

  override def customize(list: JList[? <: IncrementalityType], value: IncrementalityType,
                         index: Int, selected: Boolean, hasFocus: Boolean): Unit = {
    //noinspection ReferencePassedToNls
    setText(nameOf(value))
  }

  private def nameOf(value: IncrementalityType) = value match {
    case IncrementalityType.IDEA => "IDEA"
    case IncrementalityType.SBT  => "Zinc"
    case _                       => throw new RuntimeException(value.toString)
  }
}