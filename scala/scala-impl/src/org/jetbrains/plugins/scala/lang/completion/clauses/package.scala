package org.jetbrains.plugins.scala
package lang
package completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.{LookupElement, LookupElementBuilder, LookupElementRenderer}
import com.intellij.patterns.{ElementPattern, PatternCondition, PlatformPatterns, PsiElementPattern}
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import com.intellij.psi.{util as _, *}
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.TypeAdjuster.adjustFor
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.{ScCaseClause, ScPattern, ScTypedPattern}
import org.jetbrains.plugins.scala.lang.psi.api.base.types.{ScSimpleTypeElement, ScTypeElement}
import org.jetbrains.plugins.scala.lang.psi.api.expr.*
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.*
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createPatternFromTextWithContext
import org.jetbrains.plugins.scala.lang.psi.types.api.{FunctionType, PartialFunctionType}

package object clauses {

  /**
   * [[ClauseCompletionParameters]] is supposed to be used in places
   * where an actual [[CompletionParameters]] cannot be instantiated.
   *
   * It encapsulates information on:
   * 1) completion position;
   * 2) original file resolve scope;
   * 3) invocation count.
   */
  case class ClauseCompletionParameters(place: PsiElement,
                                        scope: GlobalSearchScope,
                                        invocationCount: Int = 1)

  def isAccessible(member: PsiMember)
                  (implicit parameters: ClauseCompletionParameters): Boolean = {
    val ClauseCompletionParameters(place, _, invocationCount) = parameters
    completion.isAccessible(member, invocationCount)(place)
  }

  import PlatformPatterns.psiElement
  import PsiElementPattern.Capture

  private[clauses] def insideCaseClause =
    psiElement.inside(classOf[ScCaseClause])

  private[clauses] def `match`: Capture[ScMatch] =
    psiElement(classOf[ScMatch])

  private[clauses] def leafWithParent(pattern: ElementPattern[? <: PsiElement]) =
    psiElement(classOf[LeafPsiElement]).withParent(pattern)

  private[clauses] def nonQualifiedReference =
    psiElement(classOf[ScReferenceExpression])
      .`with`(new PatternCondition[ScReferenceExpression]("isNonQualified") {

        override def accepts(reference: ScReferenceExpression,
                             processingContext: ProcessingContext): Boolean =
          !reference.isQualified
      })

  private[clauses] def adjustTypesOnClauses(addImports: Boolean,
                                            pairs: Iterable[(ScCaseClause, PatternComponents)]): Unit =
    adjustTypes(addImports, pairs) {
      case ScCaseClause(Some(pattern@ScTypedPattern(typeElement)), _, _) => pattern -> typeElement
    }

  private[clauses] def adjustTypes[E <: ScalaPsiElement](addImports: Boolean,
                                                         pairs: Iterable[(E, PatternComponents)])
                                                        (collector: PartialFunction[E, (ScPattern, ScTypeElement)]): Unit = {
    val findTypeElement = collector.lift
    val elements = for {
      case (element, _) <- pairs
      case (_, typeElement) <- findTypeElement(element)
    } yield typeElement

    adjustFor(
      elements.toSeq,
      addImports = addImports,
      useTypeAliases = false
    )

    for {
      case (element, components: ClassPatternComponents) <- pairs
      case (pattern, ScSimpleTypeElement.unwrapped(codeReference)) <- findTypeElement(element)

      replacement = createPatternFromTextWithContext(
        components.presentablePatternText(Right(codeReference)),
        pattern.getContext,
        pattern
      )
    } pattern.replace(replacement)
  }

  private[clauses] def expectedMatchType(`match`: ScMatch) =
    `match`.expression.flatMap(_.`type`().toOption)

  private[clauses] def expectedFunctionalType(block: ScBlockExpr) = block.expectedType().collect {
    case PartialFunctionType(_, targetType) => targetType
    case FunctionType(_, Seq(targetType)) => targetType
  }

  private[clauses] def buildLookupElement(lookupString: String,
                                          insertHandler: ClauseInsertHandler[?])
                                         (presentation: LookupElementRenderer[LookupElement]): LookupElement =
    LookupElementBuilder.create(lookupString)
      .withInsertHandler(insertHandler)
      .withRenderer(presentation)

  private[clauses] case class Inheritors(namedInheritors: List[PsiClass],
                                         isSealed: Boolean,
                                         isExhaustive: Boolean) {

    if (namedInheritors.isEmpty) throw new IllegalArgumentException("Class contract violation")
  }

  object DirectInheritors {

    import CommonClassNames.*
    import util.CommonQualifiedNames.*

    val FqnBlockList = Set(
      JAVA_LANG_OBJECT,
      JAVA_LANG_THROWABLE,
      JAVA_LANG_EXCEPTION,
      JAVA_LANG_ERROR,
      AnyRefFqn,
      AnyFqn,
      NothingFqn
    )

    def unapply(`class`: PsiClass)
               (implicit parameters: ClauseCompletionParameters): Option[Inheritors] =
      `class`.qualifiedName match {
        case fqn if FqnBlockList(fqn) => None
        case _ =>
          val isSealed = `class`.isSealed
          val (accessibleNamedInheritors, restInheritors) = directInheritors(`class`)

          implicit val ordered: Ordering[PsiClass] =
            if (isSealed) Ordering.by(_.getNavigationElement.getTextRange.getStartOffset)
            else Ordering.by(_.getName)

          accessibleNamedInheritors.sorted.toList match {
            case Nil => None
            case namedInheritors =>
              val isNotConcrete = `class` match {
                case scalaClass: ScClass => scalaClass.hasAbstractModifier
                case _ => true
              }

              val isExhaustive = isSealed && isNotConcrete && restInheritors.isEmpty
              Some(Inheritors(namedInheritors, isSealed, isExhaustive))
          }
      }

    private def directInheritors(`class`: PsiClass)
                                (implicit parameters: ClauseCompletionParameters) = {
      import scala.jdk.CollectionConverters.*
      DirectClassInheritorsSearch
        .search(`class`, parameters.scope)
        .findAll()
        .asScala
        .toIndexedSeq
        .partition { inheritor =>
          inheritor.qualifiedName != null && isAccessible(inheritor)
        }
    }
  }

  private[clauses] object Extractor {

    def unapply(`object`: ScObject): Option[ScFunctionDefinition] = `object`.membersWithSynthetic.collectFirst {
      case function: ScFunctionDefinition if function.isUnapplyMethod => function
    }
  }
}
