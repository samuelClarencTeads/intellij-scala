package org.jetbrains.plugins.scala
package lang
package completion
package lookups

import com.intellij.codeInsight.lookup.{Lookup, LookupActionProvider, LookupElement, LookupElementAction}
import com.intellij.psi.{PsiClass, PsiNamedElement}
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.{Consumer, PlatformIcons}
import org.jetbrains.plugins.scala.lang.completion.handlers.ScalaImportingInsertHandler
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression

/**
 * @author Alexander Podkhalyuzin
 */
final class ScalaImportStaticLookupActionProvider extends LookupActionProvider {

  override def fillActions(element: LookupElement,
                           lookup: Lookup,
                           consumer: Consumer[LookupElementAction]): Unit = element match {
    case element: ScalaLookupItem if element.isClassName &&
      element.getInsertHandler == null &&
      !element.getPsiElement.isInstanceOf[PsiClass] =>

      import PlatformIcons.{CHECK_ICON as checkIcon}
      val icon = if (element.shouldImport)
        EmptyIcon.create(checkIcon.getIconWidth, checkIcon.getIconHeight)
      else
        checkIcon

      consumer.consume(new LookupElementAction(icon, ScalaBundle.message("action.import.member")) {

        import LookupElementAction.Result.ChooseItem

        override def performLookupAction: ChooseItem = {
          val handler = new ScalaImportStaticLookupActionProvider.BindingInsertHandler(
            element.getPsiElement,
            element.containingClass
          )
          element.setInsertHandler(handler)
          new ChooseItem(element)
        }
      })
    case _ =>
  }
}

object ScalaImportStaticLookupActionProvider {

  private class BindingInsertHandler(private val targetElement: PsiNamedElement,
                                     override protected val containingClass: PsiClass)
    extends ScalaImportingInsertHandler(containingClass) {

    override protected def qualifyAndImport(reference: ScReferenceExpression): Unit =
      reference.bindToElement(
        targetElement,
        Some(containingClass)
      )
  }

}