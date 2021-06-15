package org.jetbrains.plugins.scala.codeInspection.methodSignature

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.jetbrains.plugins.scala.codeInspection.{ScalaInspectionBundle, ScalaQuickFixTestBase}

class MutatorLikeMethodInspectionTest extends ScalaQuickFixTestBase {

  import CodeInsightTestFixture.{CARET_MARKER as CARET}
  protected override val classOfInspection: Class[? <: LocalInspectionTool] =
    classOf[ParameterlessOverrideInspection.MutatorLikeMethod]

  protected override val description: String =
    ScalaInspectionBundle.message("method.signature.parameterless.override.mutator.like")

  private val hint = ScalaInspectionBundle.message("empty.parentheses")


  def test_explicit_type(): Unit = {
    checkTextHasError(
      text = s"def ${START}addUser$END: Boolean = true"
    )

    testQuickFix(
      text = s"def addUs${CARET}er: Boolean = true",
      expected = "def addUser(): Boolean = true",
      hint
    )
  }

  def test_implicit_type(): Unit = {
    checkTextHasError(
      text = s"def ${START}updateUser$END = true"
    )

    testQuickFix(
      text = s"def update${CARET}User = true",
      expected = "def updateUser() = true",
      hint
    )
  }

  def test_unit_type(): Unit = {
    // this is handled by another inspection
    checkTextHasNoErrors(
      text = s"def addUser: Unit = ()"
    )
  }
}
