package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern

/**
  * User: Alexander Podkhalyuzin
  * Date: 17.07.2009
  */
class ScBindingPatternStubImpl[P <: ScBindingPattern](parent: StubElement[? <: PsiElement],
                                                      elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement],
                                                      name: String)
  extends ScNamedStubBase[P](parent, elementType, name) with ScBindingPatternStub[P]