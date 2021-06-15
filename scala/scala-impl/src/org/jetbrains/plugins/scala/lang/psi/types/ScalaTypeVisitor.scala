package org.jetbrains.plugins.scala
package lang
package psi
package types

/**
 * User: Alefas
 * Date: 28.09.11
 */
trait ScalaTypeVisitor {

  import api.*
  import nonvalue.*

  def visitStdType(`type`: StdType): Unit = {}

  def visitJavaArrayType(`type`: JavaArrayType): Unit = {}

  def visitMethodType(`type`: ScMethodType): Unit = {}

  def visitUndefinedType(`type`: UndefinedType): Unit = {}

  def visitTypeParameterType(`type`: TypeParameterType): Unit = {}

  def visitParameterizedType(`type`: ParameterizedType): Unit = {}

  def visitProjectionType(p: designator.ScProjectionType): Unit = {}

  def visitThisType(t: designator.ScThisType): Unit = {}

  def visitDesignatorType(d: designator.ScDesignatorType): Unit = {}

  def visitLiteralType(l: ScLiteralType): Unit = {}

  def visitCompoundType(c: ScCompoundType): Unit = {}

  def visitExistentialType(e: ScExistentialType): Unit = {}

  def visitExistentialArgument(s: ScExistentialArgument): Unit = {}

  def visitAbstractType(a: ScAbstractType): Unit = {}

  def visitTypePolymorphicType(t: ScTypePolymorphicType): Unit = {}
}