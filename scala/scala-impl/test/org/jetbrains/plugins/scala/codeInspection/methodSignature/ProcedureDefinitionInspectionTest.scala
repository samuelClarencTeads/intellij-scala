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
class ProcedureDefinitionInspectionTest extends ScalaQuickFixTestBase {

  import CodeInsightTestFixture.CARET_MARKER
  protected override val classOfInspection: Class[? <: LocalInspectionTool] =
    classOf[UnitMethodInspection.ProcedureDefinition]

  protected override val description: String =
    ScalaInspectionBundle.message("method.signature.procedure.definition")

  private val hint = ScalaInspectionBundle.message("convert.to.function.syntax")

  def test1(): Unit = {
    checkTextHasError(
      text = s"def ${START}foo$END() {println()}"
    )

    testQuickFix(
      text = "def foo() {println()}",
      expected = "def foo(): Unit = {println()}",
      hint
    )
  }

  def test2(): Unit = {
    checkTextHasError(
      text =
        s"""def haha() {}
           |def ${START}hoho$END() {}
           |def hihi()""",
      allowAdditionalHighlights = true
    )

    testQuickFix(
      text =
        s"""def haha() {}
           |def ho${CARET_MARKER}ho() {}
           |def hihi()""",
      expected =
        """def haha() {}
          |def hoho(): Unit = {}
          |def hihi()""",
      hint
    )
  }

  def test3(): Unit = {
    checkTextHasError(
      text = s"def ${START}foo$END(x: Int) {}"
    )

    testQuickFix(
      text = "def foo(x: Int) {}",
      expected = "def foo(x: Int): Unit = {}",
      hint
    )
  }

  def test4(): Unit = {
    checkTextHasError(
      text = s"def ${START}foo$END {}"
    )

    testQuickFix(
      text = "def foo {}",
      expected = "def foo: Unit = {}",
      hint
    )
  }

  def test5(): Unit = checkTextHasNoErrors(
    text =
      """class A(val x: Int, val y: Int) {
        |    def this(x: Int) {
        |      this(x, 0)
        |    }
        |  }""".stripMargin
  )
}
