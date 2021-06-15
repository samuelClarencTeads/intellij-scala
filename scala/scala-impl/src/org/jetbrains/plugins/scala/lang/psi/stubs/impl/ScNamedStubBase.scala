package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import com.intellij.psi.stubs.*
import com.intellij.psi.{PsiElement, PsiNamedElement}

/**
  * @author adkozlov
  */
abstract class ScNamedStubBase[E <: PsiNamedElement] protected[impl](parent: StubElement[? <: PsiElement],
                                                                     elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement],
                                                                     name: String)
  extends StubBase[E](parent, elementType) with NamedStub[E] {

  override final def getName: String = name
}
