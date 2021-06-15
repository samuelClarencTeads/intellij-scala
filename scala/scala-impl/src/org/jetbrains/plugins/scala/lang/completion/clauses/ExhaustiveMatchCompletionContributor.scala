package org.jetbrains.plugins.scala
package lang
package completion
package clauses

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.patterns.{ElementPattern, PlatformPatterns}
import com.intellij.psi.*
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.*
import org.jetbrains.plugins.scala.lang.psi.api.expr.*
import org.jetbrains.plugins.scala.lang.psi.types.ScType

final class ExhaustiveMatchCompletionContributor extends ScalaCompletionContributor {

  import ExhaustiveMatchCompletionContributor.*
  import PlatformPatterns.psiElement

  extend(
    leafWithParent {
      nonQualifiedReference.withParent {
        psiElement(classOf[ScPostfixExpr]) || psiElement(classOf[ScInfixExpr])
      }
    }
  ) {
    new ExhaustiveClauseCompletionProvider[ScSugarCallExpr](ScalaKeyword.MATCH) {

      override protected def targetType(call: ScSugarCallExpr)
                                       (implicit place: PsiElement): Option[ScType] = call match {
        case _: ScPrefixExpr => None
        case ScSugarCallExpr(operand, operation, _) if operation.isAncestorOf(place) => operand.`type`().toOption
        case _ => None
      }

      override protected def createInsertHandler(strategy: PatternGenerationStrategy) =
        new ExhaustiveClauseInsertHandler[ScMatch](strategy, None, None)
    }
  }

  extend(leafWithParent(`match`)) {
    new ExhaustiveClauseCompletionProvider[ScMatch]() {

      override protected def targetType(`match`: ScMatch)
                                       (implicit place: PsiElement): Option[ScType] =
        expectedMatchType(`match`)

      override protected def createInsertHandler(strategy: PatternGenerationStrategy) =
        new ExhaustiveClauseInsertHandler[ScMatch](strategy)
    }
  }

  extend(
    leafWithParent {
      nonQualifiedReference.withParents(classOf[ScBlock], classOf[ScArgumentExprList], classOf[ScMethodCall])
    }
  ) {
    new ExhaustiveClauseCompletionProvider[ScBlockExpr]() {

      override protected def targetType(block: ScBlockExpr)
                                       (implicit place: PsiElement): Option[ScType] =
        expectedFunctionalType(block)

      override protected def createInsertHandler(strategy: PatternGenerationStrategy) =
        new ExhaustiveClauseInsertHandler[ScBlockExpr](strategy)
    }
  }

  private def extend(place: ElementPattern[? <: PsiElement])
                    (provider: ExhaustiveClauseCompletionProvider[?]): Unit =
    extend(CompletionType.BASIC, place, provider)
}

object ExhaustiveMatchCompletionContributor {

  private[lang] val Exhaustive = "exhaustive"

  private[lang] def rendererTailText = "(" + Exhaustive + ")"

  private abstract class ExhaustiveClauseCompletionProvider[
    E <: ScExpression : reflect.ClassTag,
  ](keywordLookupString: String = ScalaKeyword.CASE) extends ClauseCompletionProvider[E] {

    override final protected def addCompletions(expression: E, result: CompletionResultSet)
                                               (implicit parameters: ClauseCompletionParameters): Unit = for {
      case PatternGenerationStrategy(strategy) <- targetType(expression)(parameters.place)
      if strategy.canBeExhaustive

      lookupElement = buildLookupElement(
        keywordLookupString,
        createInsertHandler(strategy)
      ) {
        case (_, presentation: LookupElementPresentation) =>
          presentation.setItemText(keywordLookupString)
          presentation.setItemTextBold(true)

          presentation.setTailText(" ", true)
          presentation.appendTailText(rendererTailText, true)
      }
    } result.addElement(lookupElement)

    protected def targetType(expression: E)
                            (implicit place: PsiElement): Option[ScType]

    protected def createInsertHandler(strategy: PatternGenerationStrategy): ExhaustiveClauseInsertHandler[?]
  }

  private final class ExhaustiveClauseInsertHandler[
    E <: ScExpression : reflect.ClassTag
  ](strategy: PatternGenerationStrategy,
    prefix: Option[String] = Some(""),
    suffix: Option[String] = Some("")) extends ClauseInsertHandler[E] {

    override protected def handleInsert(implicit context: InsertionContext): Unit = {
      val (components, clausesText) = strategy.createClauses(prefix, suffix)
      replaceText(clausesText)

      onTargetElement { (statement: E) =>
        val caseClauses = statement.findLastChildByTypeScala[ScCaseClauses](parser.ScalaElementType.CASE_CLAUSES).get

        val clauses = caseClauses.caseClauses
        strategy.adjustTypes(components, clauses)

        reformatAndMoveCaret(caseClauses, clauses.head, statement.getTextRange)
      }
    }
  }
}
