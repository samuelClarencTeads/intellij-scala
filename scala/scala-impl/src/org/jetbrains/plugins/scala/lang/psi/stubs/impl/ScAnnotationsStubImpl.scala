package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScAnnotations

/**
  * User: Alexander Podkhalyuzin
  * Date: 22.06.2009
  */
class ScAnnotationsStubImpl(parent: StubElement[? <: PsiElement],
                            elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement])
  extends StubBase[ScAnnotations](parent, elementType) with ScAnnotationsStub