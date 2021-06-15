package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.{ScBindingPattern, ScNamingPattern, ScReferencePattern, ScSeqWildcardPattern, ScTypedPattern}
import org.jetbrains.plugins.scala.lang.psi.impl.base.patterns.{ScNamingPatternImpl, ScReferencePatternImpl, ScSeqWildcardPatternImpl, ScTypedPatternImpl}
import org.jetbrains.plugins.scala.lang.psi.stubs.impl.ScBindingPatternStubImpl

/**
  * User: Alexander Podkhalyuzin
  * Date: 17.07.2009
  */
abstract class ScBindingPatternElementType[P <: ScBindingPattern](debugName: String) extends ScStubElementType[ScBindingPatternStub[P], P](debugName) {
  override def serialize(stub: ScBindingPatternStub[P], dataStream: StubOutputStream): Unit = {
    dataStream.writeName(stub.getName)
  }

  override def deserialize(dataStream: StubInputStream, parentStub: StubElement[? <: PsiElement]): ScBindingPatternStub[P] =
    new ScBindingPatternStubImpl[P](parentStub, this, name = dataStream.readNameString)

  override protected def createStubImpl(psi: P,
                                        parentStub: StubElement[? <: PsiElement]): ScBindingPatternStub[P] =
    new ScBindingPatternStubImpl[P](parentStub, this, psi.name)
}

object ScReferencePatternElementType extends ScBindingPatternElementType[ScReferencePattern]("reference pattern") {
  override def createElement(node: ASTNode): ScReferencePattern =
    new ScReferencePatternImpl(node)

  override def createPsi(stub: ScBindingPatternStub[ScReferencePattern]): ScReferencePattern =
    new ScReferencePatternImpl(stub)
}

object ScTypedPatternElementType extends ScBindingPatternElementType[ScTypedPattern]("typed pattern") {
  override def createElement(node: ASTNode): ScTypedPattern =
    new ScTypedPatternImpl(node)

  override def createPsi(stub: ScBindingPatternStub[ScTypedPattern]): ScTypedPattern =
    new ScTypedPatternImpl(stub)
}

object ScNamingPatternElementType extends ScBindingPatternElementType[ScNamingPattern]("naming pattern") {
  override def createElement(node: ASTNode): ScNamingPattern =
    new ScNamingPatternImpl(node)

  override def createPsi(stub: ScBindingPatternStub[ScNamingPattern]): ScNamingPattern =
    new ScNamingPatternImpl(stub)
}

object ScSeqWildcardPatternElementType extends ScBindingPatternElementType[ScSeqWildcardPattern]("seq wildcard pattern") {
  override def createElement(node: ASTNode): ScSeqWildcardPattern =
    new ScSeqWildcardPatternImpl(node)

  override def createPsi(stub: ScBindingPatternStub[ScSeqWildcardPattern]): ScSeqWildcardPattern =
    new ScSeqWildcardPatternImpl(stub)
}