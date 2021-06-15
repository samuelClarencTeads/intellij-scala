package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPatternList

/**
  * User: Alexander Podkhalyuzin
  * Date: 17.07.2009
  */
class ScPatternListStubImpl(parent: StubElement[? <: PsiElement],
                            elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement],
                            override val simplePatterns: Boolean)
  extends StubBase[ScPatternList](parent, elementType) with ScPatternListStub