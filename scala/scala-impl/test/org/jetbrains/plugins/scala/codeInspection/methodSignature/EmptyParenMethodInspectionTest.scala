package org.jetbrains.plugins.scala.codeInspection.methodSignature

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.jetbrains.plugins.scala.codeInspection.{ScalaInspectionBundle, ScalaQuickFixTestBase}

class EmptyParenMethodInspectionTest extends ScalaQuickFixTestBase {

  import CodeInsightTestFixture.{CARET_MARKER as CARET}
  protected override val classOfInspection: Class[? <: LocalInspectionTool] =
    classOf[ParameterlessOverrideInspection.EmptyParenMethod]

  protected override val description: String =
    ScalaInspectionBundle.message("method.signature.parameterless.override.empty.paren")

  private val hint = ScalaInspectionBundle.message("empty.parentheses")


  def test(): Unit = {
    checkTextHasError(
      text =
        s"""
           |class Impl extends Base {
           |  def ${START}blub$END: Int = 0
           |}
           |
           |trait Base {
           |  def blub(): Int
           |}
         """.stripMargin
    )

    testQuickFix(
      text =
        s"""
           |class Impl extends Base {
           |  def bl${CARET}ub: Int = 0
           |}
           |
           |trait Base {
           |  def blub(): Int
           |}
         """.stripMargin,
      expected =
        s"""
           |class Impl extends Base {
           |  def blub(): Int = 0
           |}
           |
           |trait Base {
           |  def blub(): Int
           |}
         """.stripMargin,
      hint
    )
  }

  def test_ok(): Unit = {
    checkTextHasNoErrors(
      text =
        s"""
           |class Impl extends Base {
           |  def blub(): Int = 0
           |}
           |
           |trait Base {
           |  def blub(): Int
           |}
         """.stripMargin
    )

    checkTextHasNoErrors(
      text =
        s"""
           |class Impl extends Base {
           |  def blub: Int = 0
           |}
           |
           |trait Base {
           |  def blub: Int
           |}
         """.stripMargin
    )
  }
}
