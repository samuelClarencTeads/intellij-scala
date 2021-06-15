package org.jetbrains.plugins.scala.lang.resolve.processor

import com.intellij.psi.*
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveState.ResolveStateExt
import org.jetbrains.plugins.scala.lang.resolve.{ScalaResolveResult, StdKinds}


/**
 * @author Alexander Podkhalyuzin
 */

class CollectMethodsProcessor(place: PsiElement, name: String)
        extends ResolveProcessor(StdKinds.methodsOnly, place, name) {

  override protected def execute(namedElement: PsiNamedElement)
                                (implicit state: ResolveState): Boolean = {
    if (nameMatches(namedElement)) {
      val accessible = isAccessible(namedElement, ref)
      if (accessibility && !accessible) return true

      namedElement match {
        case method: PsiMethod =>
          addResult(new ScalaResolveResult(method,
            substitutor        = state.substitutor,
            importsUsed        = state.importsUsed,
            implicitConversion = state.implicitConversion,
            implicitType       = state.implicitType,
            isAccessible       = accessible))
        case _ =>
      }
    }

    true
  }
}