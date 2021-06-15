package org.jetbrains.plugins.scala.autoImport.quickFix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.{PsiDocCommentOwner, PsiNamedElement}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.annotator.UnresolvedReferenceFixProvider
import org.jetbrains.plugins.scala.autoImport.GlobalImplicitConversion
import org.jetbrains.plugins.scala.autoImport.quickFix.ScalaImportElementFix.isExcluded
import org.jetbrains.plugins.scala.extensions.{ChildOf, ObjectExt, PsiNamedElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScInterpolatedStringLiteral, ScReference}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScReferenceExpression, ScSugarCallExpr}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.impl.expr.ScInterpolatedExpressionPrefix
import org.jetbrains.plugins.scala.lang.psi.implicits.{ImplicitCollector, ImplicitConversionData}
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult
import org.jetbrains.plugins.scala.lang.resolve.processor.CompletionProcessor
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings

import scala.collection.mutable.ArrayBuffer

class ImportImplicitConversionFix private (ref: ScReferenceExpression,
                                           computation: ConversionToImportComputation)
  extends ScalaImportElementFix[MemberToImport](ref) {

  override protected def findElementsToImport(): Seq[MemberToImport] =
    computation.conversions

  override def createAddImportAction(editor: Editor): ScalaAddImportAction[?, ?] =
    ScalaAddImportAction.importImplicitConversion(editor, elements, ref)

  override def isAddUnambiguous: Boolean = false

  override def getText: String = elements match {
    case Seq(conversion) => ScalaBundle.message("import.with", conversion.qualifiedName)
    case _               => ScalaBundle.message("import.implicit.conversion")
  }

  override def getFamilyName: String =
    ScalaBundle.message("import.implicit.conversion")

  override def shouldShowHint(): Boolean =
    super.shouldShowHint() && ScalaApplicationSettings.getInstance().SHOW_IMPORT_POPUP_CONVERSIONS
}

private object ImportImplicitConversionFix {
  def apply(ref: ScReferenceExpression, computation: ConversionToImportComputation): ImportImplicitConversionFix =
    new ImportImplicitConversionFix(ref, computation)
}

private class ConversionToImportComputation(ref: ScReferenceExpression) {
  private case class Result(conversions: Seq[MemberToImport], missingInstances: Seq[ScalaResolveResult])

  private lazy val result: Result = {
    val visible =
      (for {
        result <- ImplicitCollector.visibleImplicits(ref)
        fun    <- result.element.asOptionOf[ScFunction]
        if fun.isImplicitConversion
      } yield fun)

    val conversionsToImport = ArrayBuffer.empty[GlobalImplicitConversion]
    val notFoundImplicits = ArrayBuffer.empty[ScalaResolveResult]

    for {
      qualifier                 <- qualifier(ref).toSeq
      case (conversion, application) <- ImplicitConversionData.getPossibleConversions(qualifier).toSeq

      if !isExcluded(conversion.qualifiedName, ref.getProject) &&
        CompletionProcessor.variantsWithName(application.resultType, qualifier, ref.refName).nonEmpty

    } {
      val notFoundImplicitParameters = application.implicitParameters.filter(_.isNotFoundImplicitParameter)

      if (visible.contains(conversion.function))
        notFoundImplicits ++= notFoundImplicitParameters
      else if (mayFindImplicits(notFoundImplicitParameters, qualifier))
        conversionsToImport += conversion
    }

    val sortedConversions = conversionsToImport.sortBy(c => (isDeprecated(c), c.qualifiedName)).toSeq

    Result(sortedConversions.map(f => MemberToImport(f.member, f.owner, f.pathToOwner)), notFoundImplicits.toSeq)
  }

  def conversions: Seq[MemberToImport] = result.conversions
  def missingImplicits: Seq[ScalaResolveResult] = result.missingInstances

  private def qualifier(ref: ScReferenceExpression): Option[ScExpression] = ref match {
    case prefix: ScInterpolatedExpressionPrefix =>
      prefix.getParent.asInstanceOf[ScInterpolatedStringLiteral]
        .desugaredExpression.flatMap(_._1.qualifier)
    case ChildOf(ScSugarCallExpr(base, refExpr: ScReferenceExpression, _)) if refExpr == ref =>
      Some(base)
    case _ =>
      ref.qualifier
  }

  private def isDeprecated(conversion: GlobalImplicitConversion): Boolean =
    isDeprecated(conversion.owner) || isDeprecated(conversion.function)

  private def isDeprecated(named: PsiNamedElement): Boolean = named.nameContext match {
    case member: PsiDocCommentOwner => member.isDeprecated
    case _                          => false
  }

  //todo we already search for implicit parameters, so we could import them together with a conversion
  // need to think about UX
  private def mayFindImplicits(notFoundImplicitParameters: Seq[ScalaResolveResult],
                               owner: ScExpression): Boolean =
    notFoundImplicitParameters.isEmpty || ImportImplicitInstanceFix.implicitsToImport(notFoundImplicitParameters, owner).nonEmpty
}

object ImportImplicitConversionFixes {

  final class Provider extends UnresolvedReferenceFixProvider {
    override def fixesFor(reference: ScReference): Seq[IntentionAction] =
      reference match {
        case refExpr: ScReferenceExpression if refExpr.isQualified                                  => ImportImplicitConversionFixes(refExpr)
        case ChildOf(ScSugarCallExpr(_, refExpr: ScReferenceExpression, _)) if refExpr == reference => ImportImplicitConversionFixes(refExpr)
        case _ => Nil
      }
  }

  def apply(ref: ScReferenceExpression): Seq[ScalaImportElementFix[? <: ElementToImport]] = {
    val computation = new ConversionToImportComputation(ref)
    Seq(ImportImplicitConversionFix(ref, computation), ImportImplicitInstanceFix(() => computation.missingImplicits, ref))
  }
}