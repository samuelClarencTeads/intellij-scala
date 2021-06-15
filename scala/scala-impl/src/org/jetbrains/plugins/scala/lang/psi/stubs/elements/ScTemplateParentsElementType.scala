package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{StubElement, StubInputStream, StubOutputStream}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScTemplateParents
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.templates.ScTemplateParentsImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.impl.ScTemplateParentsStubImpl

/**
  * User: Alexander Podkhalyuzin
  * Date: 17.06.2009
  */
final class ScTemplateParentsElementType extends ScStubElementType[ScTemplateParentsStub, ScTemplateParents]("template parents") {

  override def createElement(node: ASTNode) = new ScTemplateParentsImpl(node)

  override def createPsi(stub: ScTemplateParentsStub) = new ScTemplateParentsImpl(stub)

  override def serialize(stub: ScTemplateParentsStub,
                         dataStream: StubOutputStream): Unit = {
    dataStream.writeNames(stub.parentTypesTexts)
    dataStream.writeOptionName(stub.constructorText)
  }

  override def deserialize(dataStream: StubInputStream,
                           parentStub: StubElement[? <: PsiElement]) = new ScTemplateParentsStubImpl(
    parentStub,
    this,
    parentTypesTexts = dataStream.readNames,
    constructorText = dataStream.readOptionName
  )

  override def createStubImpl(templateParents: ScTemplateParents,
                              parentStub: StubElement[? <: PsiElement]) = new ScTemplateParentsStubImpl(
    parentStub,
    this,
    parentTypesTexts = templateParents.typeElementsWithoutConstructor.asStrings(),
    constructorText = templateParents.constructorInvocation.map(_.getText)
  )
}
