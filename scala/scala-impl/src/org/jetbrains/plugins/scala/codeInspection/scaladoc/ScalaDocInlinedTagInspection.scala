package org.jetbrains.plugins.scala
package codeInspection
package scaladoc

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createMonospaceSyntaxFromText
import org.jetbrains.plugins.scala.lang.scaladoc.parser.parsing.MyScaladocParsing
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocInlinedTag

/**
 * User: Dmitry Naidanov
 * Date: 11/21/11
 */
final class ScalaDocInlinedTagInspection extends LocalInspectionTool {
  override def isEnabledByDefault: Boolean = true

  override def getDisplayName: String = ScalaInspectionBundle.message("display.name.inlined.tag")

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    new ScalaElementVisitor {
      override def visitInlinedTag(inlinedTag: ScDocInlinedTag): Unit = {
        val problem = holder.getManager.createProblemDescriptor(
          inlinedTag, getDisplayName, true,
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly,
          new ScalaDocInlinedTagDeleteQuickFix(inlinedTag),
          new ScalaDocInlinedTagReplaceQuickFix(inlinedTag)
        )
        holder.registerProblem(problem)
      }
    }
}


final class ScalaDocInlinedTagDeleteQuickFix(inlinedTag: ScDocInlinedTag)
  extends AbstractFixOnPsiElement(ScalaBundle.message("delete.inlined.tag"), inlinedTag) {

  override def getFamilyName: String = FamilyName

  override protected def doApplyFix(tag: ScDocInlinedTag)
                                   (implicit project: Project): Unit = {
    tag.delete()
  }
}

final class ScalaDocInlinedTagReplaceQuickFix(inlinedTag: ScDocInlinedTag)
  extends AbstractFixOnPsiElement(ScalaBundle.message("replace.with.wiki.syntax"), inlinedTag) {

  override def getFamilyName: String = FamilyName

  override protected def doApplyFix(tag: ScDocInlinedTag)
                                   (implicit project: Project): Unit = {
    val text = tag.valueElement
      .map(_.getText)
      .map(_.replace("`", MyScaladocParsing.escapeSequencesForWiki("`")))
      .getOrElse("")

    tag.replace(createMonospaceSyntaxFromText(text))
  }
}