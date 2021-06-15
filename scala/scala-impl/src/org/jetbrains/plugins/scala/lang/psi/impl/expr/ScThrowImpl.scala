package org.jetbrains.plugins.scala
package lang
package psi
package impl
package expr

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.expr.*
import org.jetbrains.plugins.scala.lang.psi.types.api.Nothing
import org.jetbrains.plugins.scala.lang.psi.types.result.*

/**
  * @author Alexander Podkhalyuzin, ilyas
  */
class ScThrowImpl(node: ASTNode) extends ScExpressionImplBase(node) with ScThrow {
  protected override def innerType: TypeResult = Right(Nothing)

  override def toString: String = "ThrowStatement"
}