package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameters

/**
  * User: Alexander Podkhalyuzin
  * Date: 19.10.2008
  */
class ScParamClausesStubImpl(parent: StubElement[? <: PsiElement],
                             elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement])
  extends StubBase[ScParameters](parent, elementType) with ScParamClausesStub