package org.jetbrains.plugins.scala
package lang
package psi
package api
package statements
package params

import com.intellij.psi.*

/**
* @author Alexander Podkhalyuzin
* Date: 21.03.2008
*/

trait ScParameters extends ScalaPsiElement with PsiParameterList {

  def params: Seq[ScParameter] = clauses.flatMap((clause: ScParameterClause) => clause.parameters)

  def clauses: Seq[ScParameterClause]

  def addClause(clause: ScParameterClause): ScParameters = {
    getNode.addChild(clause.getNode)
    this
  }

  override def getParameterIndex(p: PsiParameter): Int = params.indexOf(p)

  override def getParametersCount: Int = params.length

  override def getParameters: Array[PsiParameter] = params.toArray
}