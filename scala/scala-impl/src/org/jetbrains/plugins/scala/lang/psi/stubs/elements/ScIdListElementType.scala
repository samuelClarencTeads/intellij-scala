package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package elements


import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.plugins.scala.lang.psi.api.base.ScIdList
import org.jetbrains.plugins.scala.lang.psi.impl.base.ScIdListImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.impl.ScIdListStubImpl

/**
  * User: Alexander Podkhalyuzin
  * Date: 19.07.2009
  */
class ScIdListElementType
  extends ScStubElementType[ScIdListStub, ScIdList]("id list") {
  override def deserialize(dataStream: StubInputStream, parentStub: StubElement[? <: PsiElement]): ScIdListStub =
    new ScIdListStubImpl(parentStub.asInstanceOf[StubElement[PsiElement]], this)

  override def createStubImpl(psi: ScIdList, parentStub: StubElement[? <: PsiElement]): ScIdListStub =
    new ScIdListStubImpl(parentStub, this)

  override def createElement(node: ASTNode): ScIdList = new ScIdListImpl(node)

  override def createPsi(stub: ScIdListStub): ScIdList = new ScIdListImpl(stub)
}