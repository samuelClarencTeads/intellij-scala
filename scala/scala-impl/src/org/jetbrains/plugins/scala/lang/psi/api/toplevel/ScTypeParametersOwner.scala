package org.jetbrains.plugins.scala
package lang
package psi
package api
package toplevel

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import org.jetbrains.plugins.scala.caches.ModTracker
import org.jetbrains.plugins.scala.lang.parser.ScalaElementType
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.*
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaStubBasedElementImpl
import org.jetbrains.plugins.scala.macroAnnotations.Cached

trait ScTypeParametersOwner extends ScalaPsiElement {

  @Cached(ModTracker.anyScalaPsiChange, this)
  def typeParameters: Seq[ScTypeParam] = {
    typeParametersClause match {
      case Some(clause) => clause.typeParameters
      case _ => Seq.empty
    }
  }

  def typeParametersClause: Option[ScTypeParamClause] = {
    this match {
      case st: ScalaStubBasedElementImpl[?, ?] =>
        Option(st.getStubOrPsiChild(ScalaElementType.TYPE_PARAM_CLAUSE))
      case _ =>
        findChild[ScTypeParamClause]
    }
  }

  import com.intellij.psi.scope.PsiScopeProcessor
  override def processDeclarations(processor: PsiScopeProcessor,
                                  state: ResolveState,
                                  lastParent: PsiElement,
                                  place: PsiElement): Boolean = {
    if (lastParent != null) {
      var i = 0
      while (i < typeParameters.length) {
        ProgressManager.checkCanceled()
        if (!processor.execute(typeParameters.apply(i), state)) return false
        i = i + 1
      }
    }
    true
  }
}