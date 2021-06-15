package org.jetbrains.plugins.scala.codeInsight.implicits

import com.intellij.openapi.actionSystem.*
import org.jetbrains.plugins.scala.codeInsight.ScalaCodeInsightBundle

class ShowImplicitHintsAction extends ToggleAction(
  ScalaCodeInsightBundle.message("show.implicit.hints.action.text"),
  ScalaCodeInsightBundle.message("show.implicit.hints.action.description"),
  /* icon = */ null
) {
  ImplicitShortcuts.setShortcuts(ShowImplicitHintsAction.Id, ImplicitShortcuts.EnableShortcuts)

  override def isSelected(event: AnActionEvent): Boolean = ImplicitHints.enabled

  override def setSelected(e: AnActionEvent, state: Boolean): Unit = {
    ImplicitHints.enabled = state
    ImplicitHints.updateInAllEditors()
    if (!state) {
      MouseHandler.removeEscKeyListeners()
    }
  }
}

private object ShowImplicitHintsAction {
  val Id = "Scala.ShowImplicits"
}
