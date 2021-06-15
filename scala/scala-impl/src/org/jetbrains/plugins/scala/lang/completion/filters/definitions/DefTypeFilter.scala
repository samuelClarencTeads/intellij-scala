package org.jetbrains.plugins.scala
package lang
package completion
package filters
package definitions

import com.intellij.psi.*
import com.intellij.psi.filters.ElementFilter
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.lang.completion.ScalaCompletionUtil.*
import org.jetbrains.plugins.scala.lang.psi.ScDeclarationSequenceHolder
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScCaseClause
import org.jetbrains.plugins.scala.lang.psi.api.expr.*
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.*
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.*

/** 
* @author Alexander Podkhalyuzin
* Date: 28.05.2008
*/
class DefTypeFilter extends ElementFilter {
  override def isAcceptable(element: Object, context: PsiElement): Boolean = {
    if (context.isInstanceOf[PsiComment]) return false
    val leaf = getLeafByOffset(context.getTextRange.getStartOffset, context)
    if (leaf != null) {
      val parent = leaf.getParent
      parent match {
        case _: ScReferenceExpression =>
        case _ => return false
      }
      parent.getParent match {
        case parent@(_: ScDeclarationSequenceHolder |
                     _: ScCaseClause |
                     _: ScTemplateBody |
                     _: ScClassParameter) =>
          if (awful(parent, leaf))
            return true
        case _ =>
      }
    }
    false
  }

  override def isClassAcceptable(hintClass: java.lang.Class[?]): Boolean = {
    true
  }

  @NonNls
  override def toString: String = {
    "'def', 'type' keyword filter"
  }
}