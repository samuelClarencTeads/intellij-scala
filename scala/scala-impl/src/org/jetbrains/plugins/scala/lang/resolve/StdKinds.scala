package org.jetbrains.plugins.scala
package lang
package resolve

import org.jetbrains.plugins.scala.lang.resolve.ResolveTargets.*

object StdKinds {
  val stableQualRef: ResolveTargets.ValueSet             = ValueSet(PACKAGE, OBJECT, VAL)
  val stableQualOrClass: ResolveTargets.ValueSet         = stableQualRef + CLASS
  val noPackagesClassCompletion: ResolveTargets.ValueSet = ValueSet(OBJECT, VAL, CLASS)
  val stableImportSelector: ResolveTargets.ValueSet      = ValueSet(OBJECT, VAL, VAR, METHOD, PACKAGE, CLASS)
  val stableClass: ResolveTargets.ValueSet               = ValueSet(CLASS)

  val stableClassOrObject: ResolveTargets.ValueSet = ValueSet(CLASS, OBJECT)
  val objectOrValue: ResolveTargets.ValueSet       = ValueSet(OBJECT, VAL)

  val refExprLastRef: ResolveTargets.ValueSet = ValueSet(OBJECT, VAL, VAR, METHOD)
  val refExprQualRef: ResolveTargets.ValueSet = refExprLastRef + PACKAGE

  val methodRef: ResolveTargets.ValueSet   = ValueSet(VAL, VAR, METHOD)
  val methodsOnly: ResolveTargets.ValueSet = ValueSet(METHOD)

  val valuesRef: ResolveTargets.ValueSet = ValueSet(VAL, VAR)
  val varsRef: ResolveTargets.ValueSet   = ValueSet(VAR)

  val packageRef: ResolveTargets.ValueSet = ValueSet(PACKAGE)

  val annotCtor: ResolveTargets.ValueSet = ValueSet(CLASS, ANNOTATION)
}
