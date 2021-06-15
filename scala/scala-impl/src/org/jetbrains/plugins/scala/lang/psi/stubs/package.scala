package org.jetbrains.plugins.scala
package lang
package psi

import com.intellij.psi.CommonClassNames.JAVA_LANG_OBJECT
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubElement}
import com.intellij.util.ArrayUtil.EMPTY_STRING_ARRAY
import org.jetbrains.plugins.scala.lang.psi.api.base.types.{ScCompoundTypeElement, ScDesugarizableTypeElement, ScInfixTypeElement, ScParameterizedTypeElement, ScSimpleTypeElement, ScTypeElement, ScTypeProjection}
import org.jetbrains.plugins.scala.util.CommonQualifiedNames.AnyRefFqn

package object stubs {

  private[stubs] type RawStubElement = StubElement[? <: PsiElement]
  private[stubs] type RawStubElementType = IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement]

  final def classNames(te: ScTypeElement): Array[String] = {
    val allNames = te match {
      case s: ScSimpleTypeElement => Array(s.getText)
      case p: ScParameterizedTypeElement => classNames(p.typeElement)
      case i: ScInfixTypeElement => Array(i.operation.getText)
      case c: ScCompoundTypeElement =>
        c.components.toArray.flatMap(classNames)
      case d: ScDesugarizableTypeElement =>
        d.computeDesugarizedType match {
          case Some(tp) => classNames(tp)
          case _ => EMPTY_STRING_ARRAY
        }
      case tp: ScTypeProjection => Array(tp.refName)
      case _ => EMPTY_STRING_ARRAY
    }

    allNames.filter {
      case JAVA_LANG_OBJECT | AnyRefFqn => false
      case _ => true
    }
  }

}
