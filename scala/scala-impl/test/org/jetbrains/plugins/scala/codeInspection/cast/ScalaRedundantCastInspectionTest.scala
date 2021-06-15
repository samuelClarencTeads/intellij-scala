package org.jetbrains.plugins.scala.codeInspection.cast

import com.intellij.codeInspection.LocalInspectionTool
import org.jetbrains.plugins.scala.codeInspection.ScalaQuickFixTestBase

class ScalaRedundantCastInspectionTest extends ScalaQuickFixTestBase {
  override protected val classOfInspection: Class[? <: LocalInspectionTool] =
    classOf[ScalaRedundantCastInspection]

  override protected val description = "Casting '<from>' to '<to>' is redundant"

  override protected def descriptionMatches(s: String): Boolean = s != null && s.startsWith("Casting '")

  def test_int_literal(): Unit = check("3", "Int")
  def test_string_literal(): Unit = check("\"\"", "String")

  def test_string(): Unit = checkType("String")
  def test_int(): Unit = checkType("Int")
  def test_float(): Unit = checkType("Float")
  def test_Any(): Unit = checkType("Any")

  def checkType(castType: String): Unit = check(s"(??? : $castType)", castType)
  def check(expr: String, castType: String): Unit = {
    checkTextHasError(s"val x = $expr$START.asInstanceOf[$castType]$END")
    testQuickFix(
      s"val x = $expr.asInstanceOf[$castType]",
      s"val x = $expr",
      "Remove Redundant Cast"
    )
  }
}
