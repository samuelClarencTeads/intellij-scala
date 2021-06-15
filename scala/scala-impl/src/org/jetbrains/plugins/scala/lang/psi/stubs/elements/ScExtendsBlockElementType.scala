package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScExtendsBlock
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.templates.ScExtendsBlockImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.impl.ScExtendsBlockStubImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.util.ScalaInheritors

import scala.collection.immutable.ArraySeq


/**
  * @author ilyas
  */
class ScExtendsBlockElementType extends ScStubElementType[ScExtendsBlockStub, ScExtendsBlock]("extends block") {

  override def serialize(stub: ScExtendsBlockStub, dataStream: StubOutputStream): Unit = {
    dataStream.writeNames(stub.baseClasses)
  }

  override def deserialize(dataStream: StubInputStream,
                           parentStub: StubElement[? <: PsiElement]) = new ScExtendsBlockStubImpl(
    parentStub,
    this,
    baseClasses = ArraySeq.unsafeWrapArray(dataStream.readNames)
  )

  override def createStubImpl(block: ScExtendsBlock,
                              parentStub: StubElement[? <: PsiElement]) = new ScExtendsBlockStubImpl(
    parentStub,
    this,
    baseClasses = ScalaInheritors.directSupersNames(block)
  )

  override def indexStub(stub: ScExtendsBlockStub, sink: IndexSink): Unit = {
    sink.occurrences(index.ScalaIndexKeys.SUPER_CLASS_NAME_KEY, stub.baseClasses*)
  }

  override def createElement(node: ASTNode) = new ScExtendsBlockImpl(node)

  override def createPsi(stub: ScExtendsBlockStub) = new ScExtendsBlockImpl(stub)
}