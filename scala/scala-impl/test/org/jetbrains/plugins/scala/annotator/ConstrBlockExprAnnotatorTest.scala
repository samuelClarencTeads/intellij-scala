package org.jetbrains.plugins.scala.annotator

import com.intellij.lang.Language
import org.jetbrains.plugins.scala.{Scala3Language, ScalaBundle, ScalaLanguage}
import org.jetbrains.plugins.scala.annotator.element.ElementAnnotator
import org.jetbrains.plugins.scala.base.SimpleTestCase
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.{ScalaFile, ScalaPsiElement}
import org.junit.Assert

abstract class ConstrBlockExprAnnotatorTestBase extends SimpleTestCase {

  protected def language: Language

  def test_wrong_order_auxiliary_constructors_1(): Unit = {
    val code =
      """
        |class Test {
        |  def this() = ???
        |}
        |""".stripMargin
    assertMessages(messages(code))(
      Error("???", ScalaBundle.message("constructor.invocation.expected"))
    )
  }

  def test_wrong_order_auxiliary_constructors_2(): Unit = {
    val code =
      """
        |class Test {
        |  def this() =
        |    println()
        |}
        |""".stripMargin
    assertMessages(messages(code))(
      Error("println()", ScalaBundle.message("constructor.invocation.expected"))
    )
  }

  def test_wrong_order_auxiliary_constructors_3(): Unit = {
    val code =
      """
        |class Test {
        |  def this() = {
        |    println()
        |  }
        |}
        |""".stripMargin
    assertMessages(messages(code))(
      Error("println()", ScalaBundle.message("constructor.invocation.expected"))
    )
  }

  def test_wrong_order_auxiliary_constructors_4(): Unit = {
    val code =
      """
        |class Test {
        |  def this() = {
        |    println()
        |    this()
        |  }
        |}
        |""".stripMargin
    assertMessages(messages(code))(
      Error("println()", ScalaBundle.message("constructor.invocation.expected"))
    )
  }

  def test_wrong_order_auxiliary_constructors_5(): Unit = {
    val code =
      """
        |class Test {
        |  def this() = {
        |    val x = 42
        |    this()
        |  }
        |}
        |""".stripMargin
    assertMessages(messages(code))(
      Error("val x = 42", ScalaBundle.message("constructor.invocation.expected"))
    )
  }

  def test_wrong_order_auxiliary_constructors_6(): Unit = {
    val code =
      """
        |class Test {
        |  def this() =
        |    42
        |}
        |""".stripMargin
    assertMessages(messages(code))(
      Error("42", ScalaBundle.message("constructor.invocation.expected"))
    )
  }

  def test_no_errors_1(): Unit = assertNoErrors(
    """class Test {
      |  def this(x: Int) =
      |    this()
      |}
      |""".stripMargin
  )

  def test_no_errors_2(): Unit = assertNoErrors(
    """class Test {
      |  def this(x: Int) = {
      |    this()
      |  }
      |}
      |""".stripMargin
  )

  def test_no_errors_3(): Unit = assertNoErrors(
    """class Test {
      |  def this(x: Int) = {
      |    this()
      |    println()
      |  }
      |}
      |""".stripMargin
  )

  def test_no_errors_with_comment_1(): Unit = assertNoErrors(
    """class Test {
      |  def this(x: Int) = // comment
      |    this()
      |}
      |""".stripMargin
  )

  def test_no_errors_with_comment_2(): Unit = assertNoErrors(
    """class Test {
      |  def this(x: Int) = { // comment
      |    this()
      |  }
      |}
      |""".stripMargin
  )

  def test_no_errors_with_comment_3(): Unit = assertNoErrors(
    """class Test {
      |  def this(x: Int) = { // comment
      |    this()
      |    println()
      |  }
      |}
      |""".stripMargin
  )

  private def assertNoErrors(code: String): Unit = {
    val messages0 = messages(code)
    Assert.assertEquals("expected no messages", Nil, messages0)
  }

  private def messages(code: String): List[Message] = {
    val file: ScalaFile = parseText(code, language)

    implicit val mock: AnnotatorHolderMock = new AnnotatorHolderMock(file)

    file.depthFirst().filterByType[ScalaPsiElement].foreach(ElementAnnotator.annotate(_))

    val annotations = mock.annotations
    annotations.filterNot(a => Set(
      "does not take parameters",
      "cannot resolve symbol"
    ).exists(a.message.toLowerCase.contains))
  }
}


class ConstrBlockExprAnnotatorTest_scala_2 extends ConstrBlockExprAnnotatorTestBase {
  override protected def language: Language = ScalaLanguage.INSTANCE
}

class ConstrBlockExprAnnotatorTest_scala_3 extends ConstrBlockExprAnnotatorTestBase {
  override protected def language: Language = Scala3Language.INSTANCE
}