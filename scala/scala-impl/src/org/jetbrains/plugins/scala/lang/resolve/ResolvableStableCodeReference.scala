package org.jetbrains.plugins.scala
package lang
package resolve

import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.plugins.scala.caches.BlockModificationTracker
import org.jetbrains.plugins.scala.lang.psi.api.base.*
import org.jetbrains.plugins.scala.macroAnnotations.CachedWithRecursionGuard

trait ResolvableStableCodeReference

object ResolvableStableCodeReference {

  implicit class Ext(private val stableRef: ScStableCodeReference) extends AnyVal {

    @CachedWithRecursionGuard(stableRef, ScalaResolveResult.EMPTY_ARRAY, BlockModificationTracker(stableRef))
    def resolveTypesOnly(incomplete: Boolean): Array[ScalaResolveResult] = {
      val importResolverNoMethods = new StableCodeReferenceResolver(stableRef, false, false, false) {
        override protected def getKindsFor(ref: ScStableCodeReference): Set[ResolveTargets.Value] = {
          ref.getKinds(incomplete = false) diff StdKinds.methodRef
        }
      }
      importResolverNoMethods.resolve(stableRef, incomplete)
    }

    @CachedWithRecursionGuard(stableRef, ScalaResolveResult.EMPTY_ARRAY, BlockModificationTracker(stableRef))
    def resolveMethodsOnly(incomplete: Boolean): Array[ScalaResolveResult] = {
      val importResolverNoTypes = new StableCodeReferenceResolver(stableRef, false, false, false) {
        override protected def getKindsFor(ref: ScStableCodeReference): Set[ResolveTargets.Value] = {
          ref.getKinds(incomplete = false) diff StdKinds.stableClass
        }
      }

      importResolverNoTypes.resolve(stableRef, incomplete)
    }

    @CachedWithRecursionGuard(stableRef, ScalaResolveResult.EMPTY_ARRAY, BlockModificationTracker(stableRef))
    def resolveNoConstructor: Array[ScalaResolveResult] = {
      ProgressManager.checkCanceled()
      val noConstructorResolver = new StableCodeReferenceResolver(stableRef, false, false, true)
      noConstructorResolver.resolve(stableRef, incomplete = false)
    }

    @CachedWithRecursionGuard(stableRef, ScalaResolveResult.EMPTY_ARRAY, BlockModificationTracker(stableRef))
    def resolveAllConstructors: Array[ScalaResolveResult] = {
      ProgressManager.checkCanceled()
      val resolverAllConstructors = new StableCodeReferenceResolver(stableRef, false, true, false)
      resolverAllConstructors.resolve(stableRef, incomplete = false)
    }

    @CachedWithRecursionGuard(stableRef, ScalaResolveResult.EMPTY_ARRAY, BlockModificationTracker(stableRef))
    def shapeResolve: Array[ScalaResolveResult] = {
      ProgressManager.checkCanceled()
      val shapesResolver = new StableCodeReferenceResolver(stableRef, true, false, false)
      shapesResolver.resolve(stableRef, incomplete = false)
    }

    @CachedWithRecursionGuard(stableRef, ScalaResolveResult.EMPTY_ARRAY, BlockModificationTracker(stableRef))
    def shapeResolveConstr: Array[ScalaResolveResult] = {
      ProgressManager.checkCanceled()
      val shapesResolverAllConstructors = new StableCodeReferenceResolver(stableRef, true, true, false)
      shapesResolverAllConstructors.resolve(stableRef, incomplete = false)
    }
  }
}
