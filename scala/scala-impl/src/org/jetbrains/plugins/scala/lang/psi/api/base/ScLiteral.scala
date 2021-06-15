package org.jetbrains.plugins.scala
package lang
package psi
package api
package base

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import org.jetbrains.plugins.scala.lang.psi.api.expr.*
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScalaType, api}

/**
 * @author Alexander Podkhalyuzin
 *         Date: 22.02.2008
 */
trait ScLiteral extends ScExpression
  with PsiLiteral {

  protected type V >: Null <: AnyRef

  override def getValue: V

  // TODO: now when we have ScStringLiteral we can remove isString & isMultilineString
  def isString: Boolean

  /**
   * @return true - if the string uses triple quotes<br>
   *         examples of multiline strings: {{{
   *           - """text"""
   *           - s"""text"""
   *           - """line 1
   *                |line2"""
   *           - s"""line 1
   *                  line2
   *                  line3"""
   *         }}}
   *         false - otherwise
   */
  def isMultiLineString: Boolean

  def contentRange: TextRange

  def contentText: String
}

object ScLiteral {

  trait Numeric extends ScLiteral {

    override protected type V >: Null <: Number

    private[psi] type T <: AnyVal

    private[psi] def unwrappedValue(value: V): T
  }

  private[psi] abstract class NumericCompanion[L <: Numeric] {

    final def unapply(literal: L): Option[L#T] = literal.getValue match {
      case null => None
      case value => Some(literal.unwrappedValue(value))
    }
  }

  private[psi] abstract class Value[V <: Any](val value: V) {

    def presentation: String = String.valueOf(value)

    def wideType(implicit project: Project): ScType

    protected final def cachedClass(fqn: String)
                                   (implicit project: Project): ScType =
      ElementScope(project).getCachedClass(fqn)
        .fold(api.Nothing: ScType)(ScalaType.designator)
  }
}