package org.jetbrains.plugins.scala
package lang
package psi
package impl
package base
package patterns

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.*
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.result.*

/**
* @author Alexander Podkhalyuzin
* Date: 28.02.2008
*/

class ScCompositePatternImpl(node: ASTNode) extends ScalaPsiElementImpl (node) with ScPatternImpl with ScCompositePattern {
  override def isIrrefutableFor(t: Option[ScType]): Boolean = subpatterns.exists(_.isIrrefutableFor(t))

  override def toString: String = "CompositePattern"

  override def `type`(): TypeResult = {
    this.expectedType match {
      case Some(expected) => Right(expected)
      case _ => super.`type`() //Failure
    }
  }
}