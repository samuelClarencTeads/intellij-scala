package org.jetbrains.plugins.scala
package codeInspection
package methodSignature

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture

/**
  * Nikolay.Tropin
  * 6/25/13
  */
class ProcedureDeclarationInspectionTest extends ScalaQuickFixTestBase {

  import CodeInsightTestFixture.CARET_MARKER
  protected override val classOfInspection: Class[? <: LocalInspectionTool] =
    classOf[UnitMethodInspection.ProcedureDeclaration]

  protected override val description: String = ScalaInspectionBundle.message("method.signature.procedure.declaration")

  private val hint = ScalaInspectionBundle.message("convert.to.function.syntax")

  def test1(): Unit = {
    checkTextHasError(s"def ${START}foo$END()")

    testQuickFix(
      "def foo()",
      "def foo(): Unit"
    )
  }

  def test2(): Unit = {
    checkTextHasError(
      s"""def haha()
         |def ${START}hoho$END()
         |def hihi()""",
      allowAdditionalHighlights = true
    )

    testQuickFix(
      s"""def haha()
         |def ho${CARET_MARKER}ho()
         |def hihi()""",
      """def haha()
        |def hoho(): Unit
        |def hihi()
      """
    )
  }

  def test3(): Unit = {
    checkTextHasError(s"def ${START}foo$END(x: Int)")
    testQuickFix(
      "def foo(x: Int)",
      "def foo(x: Int): Unit"
    )
  }

  def test4(): Unit = {
    checkTextHasError(s"def ${START}foo$END")
    testQuickFix(
      "def foo",
      "def foo: Unit"
    )
  }

  private def testQuickFix(text: String, expected: String): Unit = testQuickFix(text, expected, hint)
}
