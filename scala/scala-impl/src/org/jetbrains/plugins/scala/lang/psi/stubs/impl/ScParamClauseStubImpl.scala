
package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameterClause

/**
  * User: Alexander Podkhalyuzin
  * Date: 19.10.2008
  */
class ScParamClauseStubImpl(parent: StubElement[? <: PsiElement],
                            elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement],
                            override val isImplicit: Boolean,
                            override val isUsing: Boolean)
  extends StubBase[ScParameterClause](parent, elementType) with ScParamClauseStub