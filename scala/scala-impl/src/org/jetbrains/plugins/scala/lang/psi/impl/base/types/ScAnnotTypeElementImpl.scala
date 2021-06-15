package org.jetbrains.plugins.scala
package lang
package psi
package impl
package base
package types

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base.types.*

/**
 * @author Alexander Podkhalyuzin, ilyas
 */

class ScAnnotTypeElementImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScAnnotTypeElement {
  override protected def acceptScala(visitor: ScalaElementVisitor): Unit = {
    visitor.visitAnnotTypeElement(this)
  }
}