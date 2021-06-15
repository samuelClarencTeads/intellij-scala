package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParamClause

/**
  * User: Alexander Podkhalyuzin
  * Date: 17.06.2009
  */
class ScTypeParamClauseStubImpl(parent: StubElement[? <: PsiElement],
                                elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement],
                                override val typeParameterClauseText: String)
  extends StubBase[ScTypeParamClause](parent, elementType) with ScTypeParamClauseStub