package org.jetbrains.plugins.scala.lang.psi.impl.base.types

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.TokenSets.*
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.api.base.types.*

/** 
* @author Alexander Podkhalyuzin
* Date: 22.02.2008
*/

class ScTypeArgsImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScTypeArgs {
  override def toString: String = "TypeArgumentsList"

  override def typeArgs: Seq[ScTypeElement] = getChildren.toSeq
    .filter(e => TYPE_ELEMENTS_TOKEN_SET.contains(e.getNode.getElementType))
    .map(_.asInstanceOf[ScTypeElement])
}