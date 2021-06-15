package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScTemplateBody

/**
  * User: Alexander Podkhalyuzin
  * Date: 17.06.2009
  */
class ScTemplateBodyStubImpl(parent: StubElement[? <: PsiElement],
                             elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement])
  extends StubBase[ScTemplateBody](parent, elementType) with ScTemplateBodyStub