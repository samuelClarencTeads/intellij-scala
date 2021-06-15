package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package elements

import com.intellij.lang.{ASTNode, Language}
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.stubs.*
import org.jetbrains.plugins.scala.lang.parser.ScalaElementType.ScExpressionElementType
import org.jetbrains.plugins.scala.lang.parser.{ScCodeBlockElementType, SelfPsiCreator}
import org.jetbrains.plugins.scala.lang.psi.stubs.elements.ScStubElementType.isLocal

import scala.annotation.tailrec

/**
 * @author ilyas
 */
abstract class ScStubElementType[
  S <: StubElement[T],
  T <: PsiElement
](debugName: String,
  language: Language = ScalaLanguage.INSTANCE)
  extends IStubElementType[S, T](debugName, language)
    with SelfPsiCreator {

  override def createElement(node: ASTNode): T

  override final def createStub(psi: T, parentStub: StubElement[? <: PsiElement]): S = {
    ScStubElementType.Processing {
      createStubImpl(psi, parentStub)
    }
  }

  protected def createStubImpl(psi: T, parentStub: StubElement[? <: PsiElement]): S

  override final def getExternalId: String = getLanguage.getDisplayName.toLowerCase + "." + debugName

  override def indexStub(stub: S, sink: IndexSink): Unit = {}

  override def serialize(stub: S, dataStream: StubOutputStream): Unit = {}

  override final def isLeftBound = true

  override final def toString: String = super.toString

  override def shouldCreateStub(node: ASTNode): Boolean = !isLocal(node)
}

object ScStubElementType {

  // TODO is supposed to be eliminated eventually
  abstract class Impl[
    S >: Null <: StubElement[T],
    T <: PsiElement
  ](debugName: String,
    language: Language = ScalaLanguage.INSTANCE)
    extends ScStubElementType[S, T](debugName, language) {

    protected def createPsi(stub: S,
                            nodeType: this.type,
                            node: ASTNode,
                            debugName: String): T

    override final def createElement(node: ASTNode): T = createPsi(null, null, node, toString)

    override final def createPsi(stub: S): T = createPsi(stub, this, null, toString)
  }

  @tailrec
  private def isLocal(node: ASTNode): Boolean = node match {
    case _: FileElement | null => false
    case _ =>
      node.getElementType match {
        case _: ScTemplateDefinitionElementType[?] => false
        case _: ScExpressionElementType | _: ScCodeBlockElementType => true
        case _ => isLocal(node.getTreeParent)
      }
  }

  object Processing {

    private[this] val flag = ThreadLocal.withInitial[Long](() => 0)

    def apply[R](action: => R): R =
      try {
        this (()) += 1
        action
      } finally {
        this (()) -= 1
      }

    implicit def asBoolean(self: this.type): Boolean = apply(()) > 0

    private[this] def apply(unit: Unit): Long = flag.get

    private[this] def update(unit: Unit, long: Long): Unit = flag.set(long)
  }

}