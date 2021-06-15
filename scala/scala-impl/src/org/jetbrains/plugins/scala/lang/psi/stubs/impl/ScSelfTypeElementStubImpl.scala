package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScSelfTypeElement

/**
  * User: Alexander Podkhalyuzin
  * Date: 19.06.2009
  */
class ScSelfTypeElementStubImpl(parent: StubElement[? <: PsiElement],
                                elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement],
                                name: String,
                                override val typeText: Option[String],
                                override val classNames: Array[String])
  extends ScNamedStubBase[ScSelfTypeElement](parent, elementType, name) with ScSelfTypeElementStub