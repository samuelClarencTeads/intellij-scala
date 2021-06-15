package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter

/**
  * User: Alexander Podkhalyuzin
  * Date: 19.10.2008
  */
class ScParameterStubImpl(parent: StubElement[? <: PsiElement],
                          elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement],
                          name: String,
                          override val typeText: Option[String],
                          override val isStable: Boolean,
                          override val isDefaultParameter: Boolean,
                          override val isRepeated: Boolean,
                          override val isVal: Boolean,
                          override val isVar: Boolean,
                          override val isCallByNameParameter: Boolean,
                          override val bodyText: Option[String],
                          override val deprecatedName: Option[String],
                          override val implicitClassNames: Array[String])
  extends ScNamedStubBase[ScParameter](parent, elementType, name) with ScParameterStub