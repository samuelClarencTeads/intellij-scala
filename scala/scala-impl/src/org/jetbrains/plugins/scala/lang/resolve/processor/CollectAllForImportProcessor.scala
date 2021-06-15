package org.jetbrains.plugins.scala
package lang
package resolve
package processor

import com.intellij.psi.*
import org.jetbrains.plugins.scala.lang.psi.impl.ScPackageImpl
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveState.ResolveStateExt


class CollectAllForImportProcessor(override val kinds: Set[ResolveTargets.Value],
                                   override val ref: PsiElement,
                                   override val name: String)
  extends ResolveProcessor(kinds, ref, name) {

  override protected def execute(namedElement: PsiNamedElement)
                                (implicit state: ResolveState): Boolean = {
    if (nameMatches(namedElement)) {
      val accessible = isAccessible(namedElement, ref)
      if (accessibility && !accessible) return true
      val (target, fromType) = namedElement match {
        case pack: PsiPackage => (ScPackageImpl(pack), None)
        case _ => (namedElement, state.fromType)
      }

      candidatesSet = candidatesSet union Set(
        new ScalaResolveResult(target, state.substitutor, state.importsUsed, fromType = fromType, isAccessible = true)
      )
    }

    true
  }
}
