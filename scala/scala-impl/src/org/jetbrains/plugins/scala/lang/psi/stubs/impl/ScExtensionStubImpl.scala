package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScExtension

class ScExtensionStubImpl(parent: StubElement[? <: PsiElement],
                          elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement])
  extends StubBase[ScExtension](parent, elementType)
    with ScExtensionStub
