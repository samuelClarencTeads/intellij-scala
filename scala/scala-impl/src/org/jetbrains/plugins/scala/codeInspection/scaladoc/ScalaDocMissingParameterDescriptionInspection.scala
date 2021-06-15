package org.jetbrains.plugins.scala
package codeInspection
package scaladoc

import org.jetbrains.plugins.scala.lang.TokenSets.TokenSetExt
import com.intellij.codeInspection.{LocalInspectionTool, ProblemHighlightType, ProblemsHolder}
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.tree.TokenSet
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocTokenType
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocTag
import ScalaDocMissingParameterDescriptionInspection.*


class ScalaDocMissingParameterDescriptionInspection extends LocalInspectionTool {

  override def getDisplayName: String = ScalaInspectionBundle.message("display.name.missing.parameter.description")

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = {
    new ScalaElementVisitor {
      override def visitTag(tag: ScDocTag): Unit = {
        if (!TagsWithValueElement.contains(tag.name))
          return

        val valueElement = tag.getValueElement
        if (valueElement == null)
          return

        val hasDescription = !valueElement.nextSiblings.forall(el => NonDataTokens.contains(el.elementType))

        if (!hasDescription) {
          holder.registerProblem(holder.getManager.createProblemDescriptor(
            valueElement, getDisplayName, true,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly
          ))
        }
      }
    }
  }
}


object ScalaDocMissingParameterDescriptionInspection {
  import org.jetbrains.plugins.scala.lang.scaladoc.parser.parsing.MyScaladocParsing.*

  private val TagsWithValueElement = Set(PARAM_TAG, THROWS_TAG, TYPE_PARAM_TAG)
  private val NonDataTokens = TokenSet.create(ScalaDocTokenType.DOC_WHITESPACE, ScalaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS)
}
