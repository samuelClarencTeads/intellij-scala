package org.jetbrains.plugins.scala
package codeInsight
package template
package impl

import com.intellij.psi.*
import org.jetbrains.plugins.scala.codeInsight.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

/**
  * @author Alefas
  * @since 18/12/14.
  */
final class ScalaCommentContextType extends ScalaFileTemplateContextType.ElementContextType("COMMENT", ScalaCodeInsightBundle.message("element.context.type.comment")) {

  override protected def isInContext(offset: Int)
                                    (implicit file: ScalaFile): Boolean =
    ScalaCommentContextType.isInContext(offset)
}

object ScalaCommentContextType {

  private[impl] def isInContext(offset: Int)
                               (implicit file: ScalaFile): Boolean = {
    val elementAtOffset = file.findElementAt(offset) match {
      case _: PsiWhiteSpace if offset > 0 => file.findElementAt(offset - 1)
      case element => element
    }

    util.PsiTreeUtil.getNonStrictParentOfType(elementAtOffset, classOf[PsiComment]) != null
  }
}
