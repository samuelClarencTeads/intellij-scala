package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScAccessModifier

/**
  * User: Alexander Podkhalyuzin
  * Date: 17.06.2009
  */
class ScAccessModifierStubImpl(parent: StubElement[? <: PsiElement],
                               elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement],
                               override val isProtected: Boolean,
                               override val isPrivate: Boolean,
                               override val isThis: Boolean,
                               override val idText: Option[String])
  extends StubBase[ScAccessModifier](parent, elementType) with ScAccessModifierStub