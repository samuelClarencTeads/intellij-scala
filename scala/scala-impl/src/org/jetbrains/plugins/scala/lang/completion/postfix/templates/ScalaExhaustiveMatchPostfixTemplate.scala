package org.jetbrains.plugins.scala.lang
package completion
package postfix
package templates

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.postfix.templates.{PostfixTemplate, StringBasedPostfixTemplate}
import com.intellij.openapi.editor.{Document, Editor}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.lang.completion.clauses.{ClauseCompletionParameters, ExhaustiveMatchCompletionContributor, PatternGenerationStrategy}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScBlock, ScExpression, ScFunctionExpr, ScMatch}
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable

/**
 * @see [[ScalaMatchPostfixTemplate]]
 */
final class ScalaExhaustiveMatchPostfixTemplate(
  alias: String = ScalaExhaustiveMatchPostfixTemplate.alias
) extends PostfixTemplate(
  null,
  alias,
  s"${StringBasedPostfixTemplate.EXPR} ${ScalaExhaustiveMatchPostfixTemplate.alias} ${ScalaExhaustiveMatchPostfixTemplate.exhaustiveAlias}",
  null
) {

  import ScalaExhaustiveMatchPostfixTemplate.*

  override def isApplicable(context: PsiElement,
                            document: Document,
                            offset: Int): Boolean =
    topMostStrategy(context).isDefined

  override def expand(context: PsiElement, editor: Editor): Unit =
    for {
      case (expression, strategy) <- topMostStrategy(context)
    } expandForStrategy(expression, strategy)(context.getProject, editor)

  override def isEditable: Boolean = false
}

object ScalaExhaustiveMatchPostfixTemplate {

  private[postfix] def alias = ScalaKeyword.MATCH

  private[postfix] def exhaustiveAlias = ExhaustiveMatchCompletionContributor.Exhaustive

  private def topMostStrategy(context: PsiElement): Option[(ScExpression, PatternGenerationStrategy)] =
    PsiTreeUtil.getNonStrictParentOfType(context, classOf[ScExpression]) match {
      case null |
           _: ScBlock |
           _: ScFunctionExpr => None
      case expression =>
        implicit val parameters: ClauseCompletionParameters = ClauseCompletionParameters(expression, expression.getContainingFile.getResolveScope)
        expression match {
          case Typeable(PatternGenerationStrategy(strategy)) => Some(expression, strategy)
          case _ => None
        }
    }

  private def expandForStrategy(expression: ScExpression,
                                strategy: PatternGenerationStrategy)
                               (implicit project: Project, editor: Editor): Unit = {
    val (components, clausesText) = strategy.createClauses()
    val file = expression.getContainingFile
    val expressionText = expression.getText

    removeRange(expression.getTextRange)
    startTemplate(expressionText, clausesText)

    for {
      statement <- findMatchStatementAtCaret(file)
      caseClauses = statement.clauses
    } strategy.adjustTypes(components, caseClauses)
  }


  private[this] def removeRange(range: TextRange)
                               (implicit editor: Editor): Unit =
    editor.getDocument.deleteString(range.getStartOffset, range.getEndOffset)

  private[this] def startTemplate(expressionText: String, clausesText: String)
                                 (implicit project: Project, editor: Editor): Unit = {
    val templateString = s"$expressionText $clausesText"

    val manager = TemplateManager.getInstance(project)
    val template = manager.createTemplate("", "", templateString)
    template.setToReformat(true)
    manager.startTemplate(editor, template)
  }

  private[this] def findMatchStatementAtCaret(file: PsiFile)
                                             (implicit editor: Editor) =
    file.findElementAt(editor.getCaretModel.getOffset - 1) match {
      case null => None
      case element => Option(PsiTreeUtil.getContextOfType(element, classOf[ScMatch]))
    }
}