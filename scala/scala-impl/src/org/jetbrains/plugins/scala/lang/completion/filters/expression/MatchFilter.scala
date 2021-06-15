package org.jetbrains.plugins.scala
package lang
package completion
package filters.expression

import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.{PsiElement, *}
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.lang.completion.ScalaCompletionUtil.*
import org.jetbrains.plugins.scala.lang.psi.api.expr.*
import extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral

/** 
* @author Alexander Podkhalyuzin
* Date: 28.05.2008
*/

class MatchFilter extends ElementFilter {
  override def isAcceptable(element: Object, context: PsiElement): Boolean = {
    if (context.is[PsiComment]) return false
    val leaf = getLeafByOffset(context.getTextRange.getStartOffset, context)
    if (leaf != null) {
      val parent = leaf.getParent
      if (parent.is[ScExpression] &&
        !parent.is[ScStringLiteral] &&
        (parent.getParent.is[ScInfixExpr] ||
          (parent.getParent.is[ScPostfixExpr] &&
            !parent.getParent.getParent.is[ScTry]))) {
        return true
      }
    }
    false
  }

  override def isClassAcceptable(hintClass: java.lang.Class[?]): Boolean = {
    true
  }

  @NonNls
  override def toString: String = {
    "'match' keyword filter"
  }
}