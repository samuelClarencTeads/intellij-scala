package org.jetbrains.plugins.scala
package lang
package completion
package filters.expression

import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.{PsiElement, *}
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.lang.completion.ScalaCompletionUtil.*
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.ScalaElementType
import org.jetbrains.plugins.scala.lang.psi.api.expr.*
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScArguments

/** 
* @author Alexander Podkhalyuzin
* Date: 22.05.2008
*/

class FinallyFilter extends ElementFilter{
  override def isAcceptable(element: Object, context: PsiElement): Boolean = {
    if (context.isInstanceOf[PsiComment]) return false
    val leaf = getLeafByOffset(context.getTextRange.getStartOffset, context)
    if (leaf != null) {
      var i = getPrevNotWhitespaceAndComment(context.getTextRange.getStartOffset - 1, context)
      var leaf1 = getLeafByOffset(i, context)
      if (leaf1 == null || leaf1.getNode.getElementType == ScalaTokenTypes.kTRY) return false
      val prevIsRBrace = leaf1.textMatches("}")
      val prevIsRParan = leaf1.textMatches(")")
      while (leaf1 != null && !leaf1.isInstanceOf[ScTry]) {
        leaf1 match {
          case _: ScFinallyBlock =>
            return false
          case _: ScParenthesisedExpr | _: ScArguments if !prevIsRParan =>
            return false
          case _: ScBlock if !prevIsRBrace =>
            return false
          case _ =>
        }
        leaf1 = leaf1.getParent
      }
      if (leaf1 == null) return false
      if (leaf1.getNode.getChildren(null).exists(_.getElementType == ScalaElementType.FINALLY_BLOCK)) return false
      i = getNextNotWhitespaceAndComment(context.getTextRange.getEndOffset, context)
      if (Array("catch", "finally").contains(getLeafByOffset(i, context).getText)) return false
      return true
    }
    false
  }

  override def isClassAcceptable(hintClass: java.lang.Class[?]): Boolean = {
    true
  }

  @NonNls
  override def toString: String = {
    "statements keyword filter"
  }
}
