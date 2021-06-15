package org.jetbrains.plugins.scala.lang.psi.implicits

import com.intellij.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.caches.BlockModificationTracker
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, childOf}
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScCaseClause
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScMethodLike, ScPrimaryConstructor}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScForBinding, ScFunctionExpr, ScGenerator}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.{ScParameter, ScParameters}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScPackaging
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportStmt
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScTemplateParents
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScMember
import org.jetbrains.plugins.scala.lang.psi.api.{ImplicitArgumentsOwner, ScalaFile}
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult
import org.jetbrains.plugins.scala.macroAnnotations.CachedWithRecursionGuard
import org.jetbrains.plugins.scala.project.*

/**
  * @author Nikolay.Tropin
  */
case class ImplicitSearchScope(representative: PsiElement) {

  @CachedWithRecursionGuard(representative, Set.empty, BlockModificationTracker(representative))
  def cachedVisibleImplicits: Set[ScalaResolveResult] = {
    new ImplicitParametersProcessor(representative, withoutPrecedence = false)
      .candidatesByPlace
  }

  @CachedWithRecursionGuard(representative, Set.empty, BlockModificationTracker(representative))
  def cachedImplicitsByType(scType: ScType): Set[ScalaResolveResult] =
    new ImplicitParametersProcessor(representative, withoutPrecedence = true)
      .candidatesByType(scType)
}

object ImplicitSearchScope {
  //should be different for two elements if they have different sets of available implicit names
  def forElement(e: PsiElement): ImplicitSearchScope = {
    e.getContainingFile match {
      case _: ScalaFile => new ImplicitSearchScope(representative(e).getOrElse(e))
      case _ => new ImplicitSearchScope(e)
    }
  }

  private def representative(e: PsiElement): Option[PsiElement] = {
    val elements = e.withContexts
      .takeWhile(e => e != null && !e.isInstanceOf[PsiFile])
      .flatMap(_.withPrevSiblings)

    var nonBorder: Option[PsiElement] = None
    while (elements.hasNext) {
      val next = elements.next()
      if (isImplicitSearchBorder(next))
        return nonBorder
      else if (maySearchImplicitsFor(next)) {
        nonBorder = Some(next)
      }
    }
    nonBorder
  }

  private def maySearchImplicitsFor(element: PsiElement): Boolean =
    element.isInstanceOf[ImplicitArgumentsOwner]

  private def isImplicitSearchBorder(elem: PsiElement): Boolean = elem match {
    //special treatment for case clauses and for comprehensions to
    //support `implicit0` from betterMonadicFor compiler plugin
    case _: ScForBinding if elem.betterMonadicForEnabled   => true
    case _: ScGenerator  if elem.betterMonadicForEnabled   => true
    case _: ScCaseClause if elem.betterMonadicForEnabled   => true
    case _: ScImportStmt | _: ScPackaging                  => true
    case (_: ScParameters) childOf (m: ScMethodLike)       => hasImplicitClause(m)
    case pc: ScPrimaryConstructor                          => hasImplicitClause(pc)
    case (ps: ScParameters) childOf (_: ScFunctionExpr)    => ps.params.exists(_.isImplicitParameter)
    case p: ScParameter                                    => p.isImplicitParameter
    case m: ScMember                                       => m.hasModifierProperty("implicit")
    case _: ScTemplateParents                              => true
    case _                                                 => false
  }

  private def hasImplicitClause(m: ScMethodLike): Boolean = m.effectiveParameterClauses.exists(_.isImplicit)
}