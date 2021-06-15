package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.lexer.ScalaModifier
import org.jetbrains.plugins.scala.lang.psi.api.base.ScModifierList
import org.jetbrains.plugins.scala.util.EnumSet.EnumSet

/**
  * User: Alexander Podkhalyuzin
  * Date: 21.01.2009
  */
class ScModifiersStubImpl(parent: StubElement[? <: PsiElement],
                          elemType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement],
                          override val modifiers: EnumSet[ScalaModifier])
  extends StubBase[ScModifierList](parent, elemType) with ScModifiersStub
