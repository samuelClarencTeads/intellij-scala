package org.jetbrains.plugins.scala
package lang
package completion
package filters.expression

import com.intellij.psi.*
import com.intellij.psi.filters.ElementFilter
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.extensions.PsiFileExt
import org.jetbrains.plugins.scala.lang.completion.ScalaCompletionUtil.*
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.expr.*

/**
* @author Alexander Podkhalyuzin
* Date: 28.05.2008
*/

class YieldFilter extends ElementFilter {
  private def leafText(i: Int,  context: PsiElement): String = {
    val elem = getLeafByOffset(i, context)
    if (elem == null) return ""
    elem.getText
  }

  override def isAcceptable(element: Object, context: PsiElement): Boolean = {
    if (context.isInstanceOf[PsiComment]) return false
    val leaf = getLeafByOffset(context.getTextRange.getStartOffset, context)
    if (leaf != null) {
      val parent = leaf.getParent
      if (parent.isInstanceOf[ScExpression] && parent.getParent.isInstanceOf[ScFor]) {
        val file = context.getContainingFile
        val fileText = file.charSequence
        var i = context.getTextRange.getStartOffset - 1
        while (i > 0 && (fileText.charAt(i) == ' ' || fileText.charAt(i) == '\n')) {
          i = i - 1
        }
        if (leafText(i, context) == "yield") return false
        i = context.getTextRange.getEndOffset
        while (i < fileText.length - 1 && (fileText.charAt(i) == ' ' || fileText.charAt(i) == '\n')) {
          i = i + 1
        }
        if (leafText(i, context) == "yield") return false
        for (child <- parent.getParent.getNode.getChildren(null) if child.getElementType == ScalaTokenTypes.kYIELD) return false
        return ScalaCompletionUtil.checkAnyWith(parent.getParent, "yield true", context.getManager) ||
          ScalaCompletionUtil.checkReplace(parent.getParent, "yield", context.getManager)
      }
    }
    false
  }

  override def isClassAcceptable(hintClass: java.lang.Class[?]): Boolean = {
    true
  }

  @NonNls
  override def toString: String = {
    "'yield' keyword filter"
  }
}