package org.jetbrains.plugins.scala
package codeInspection
package caseClasses

import com.intellij.codeInspection.LocalInspectionTool
import org.jetbrains.plugins.scala.codeInspection.caseClassParamInspection.CaseClassParamInspection

/**
  * @author Nikolay.Tropin
  */
class CaseClassParameterInspectionTest extends ScalaQuickFixTestBase {
  override protected val classOfInspection: Class[? <: LocalInspectionTool] = classOf[CaseClassParamInspection]

  override protected val description: String = ScalaBundle.message("val.on.case.class.param.redundant")

  def testSimpleParam(): Unit = checkTextHasError(s"case class A(${START}val$END x: Int)")

  def testSecondClause(): Unit = checkTextHasNoErrors(s"case class A(x: Int)(val s: String)")

  def testDefault(): Unit = {
    val text = s"case class A(${START}val$END x: Int = 1)"

    checkTextHasError(text)
    testQuickFix(text, "case class A(x: Int = 1)", ScalaBundle.message("remove.val"))
  }

  def testWithModifier(): Unit = checkTextHasNoErrors("case class A(protected val x: Int)")
}
