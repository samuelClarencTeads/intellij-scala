package org.jetbrains.plugins.scala.lang.psi.impl.base.patterns

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.*
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.result.*

class ScWildcardPatternImpl(node: ASTNode) extends ScalaPsiElementImpl (node) with ScPatternImpl with ScWildcardPattern {

  override def isIrrefutableFor(t: Option[ScType]): Boolean = true

  override def toString: String = "WildcardPattern"

  override def `type`(): TypeResult = this.expectedType match {
    case Some(x) => Right(x)
    case _ => Failure(ScalaBundle.message("cannot.determine.expected.type"))
  }
}