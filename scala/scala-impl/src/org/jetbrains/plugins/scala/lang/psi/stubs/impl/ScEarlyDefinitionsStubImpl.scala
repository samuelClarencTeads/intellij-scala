package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScEarlyDefinitions

/**
  * User: Alexander Podkhalyuzin
  * Date: 17.06.2009
  */
class ScEarlyDefinitionsStubImpl(parent: StubElement[? <: PsiElement],
                                 elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement])
  extends StubBase[ScEarlyDefinitions](parent, elementType) with ScEarlyDefinitionsStub