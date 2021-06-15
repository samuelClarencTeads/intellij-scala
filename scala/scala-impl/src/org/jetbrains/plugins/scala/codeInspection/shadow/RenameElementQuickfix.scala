package org.jetbrains.plugins.scala.codeInspection.shadow

import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.impl.text.TextEditorPsiDataProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.refactoring.actions.RenameElementAction
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.codeInspection.AbstractFixOnPsiElement
import org.jetbrains.plugins.scala.extensions.*

import scala.annotation.nowarn

/**
 * User: Alefas
 * Date: 06.02.12
 */
class RenameElementQuickfix(myRef: PsiElement, @Nls name: String) extends AbstractFixOnPsiElement(name, myRef) {

  override protected def doApplyFix(element: PsiElement)
                                   (implicit project: Project): Unit = {
    val action: AnAction = new RenameElementAction
    val event: AnActionEvent = actionEventForElement(element, action)
    invokeLater {
      action.actionPerformed(event)
    }
  }

  private def actionEventForElement(ref: PsiElement, action: AnAction)
                                   (implicit project: Project): AnActionEvent = {
    val builder = SimpleDataContext.builder()
    val containingFile = ref.getContainingFile
    @nowarn("cat=deprecation") val editor: Editor = InjectedLanguageUtil.openEditorFor(containingFile, project)
    if (editor.is[EditorWindow]) {
      builder.add(CommonDataKeys.EDITOR, editor)
      builder.add(CommonDataKeys.PSI_ELEMENT, ref)
    } else if (ApplicationManager.getApplication.isUnitTestMode) {
      val element = new TextEditorPsiDataProvider().getData(CommonDataKeys.PSI_ELEMENT.getName,
        editor, editor.getCaretModel.getCurrentCaret).asInstanceOf[PsiElement]
      builder.add(CommonDataKeys.PSI_ELEMENT, element)
    }
    val dataContext = builder.build()
    new AnActionEvent(null, dataContext, "", action.getTemplatePresentation, ActionManager.getInstance, 0)
  }
}
