package org.jetbrains.plugins.scala.worksheet.integration.repl

import com.intellij.openapi.editor.Editor
import org.jetbrains.plugins.scala.util.runners.*
import org.jetbrains.plugins.scala.worksheet.integration.WorksheetRuntimeExceptionsTests
import org.jetbrains.plugins.scala.worksheet.ui.printers.WorksheetEditorPrinterRepl
import org.jetbrains.plugins.scala.{LatestScalaVersions, ScalaVersion, WorksheetEvaluationTests}
import org.junit.Assert.*
import org.junit.experimental.categories.Category

import scala.language.postfixOps

@RunWithScalaVersions(Array(
  TestScalaVersion.Scala_3_0,
))
@Category(Array(classOf[WorksheetEvaluationTests]))
class WorksheetReplIntegration_Scala_3_Test extends WorksheetReplIntegrationBaseTest
  with WorksheetRuntimeExceptionsTests {

  override protected def supportedIn(version: ScalaVersion): Boolean = version > LatestScalaVersions.Scala_2_10

  def testAllInOne(): Unit = {
    val before =
      """import java.io.PrintStream
        |import scala.concurrent.duration._;
        |import scala.collection.Seq;
        |
        |println(Seq(1, 2, 3))
        |println(1)
        |
        |()
        |23
        |"str"
        |
        |def foo = "123" + 1
        |def foo0 = 1
        |def foo1() = 1
        |def foo2: Int = 1
        |def foo3(): Int = 1
        |def foo4(p: String) = 1
        |def foo5(p: String): Int = 1
        |def foo6(p: String, q: Short): Int = 1
        |def foo7[T] = 1
        |def foo8[T]() = 1
        |def foo9[T]: Int = 1
        |def foo10[T](): Int = 1
        |def foo11[T](p: String) = 1
        |def foo12[T](p: String): Int = 1
        |def foo13[T](p: String, q: Short): Int = 1
        |
        |val _ = 1
        |val x = 2
        |val y = x.toString + foo
        |val x2: PrintStream = null
        |val q1 = new DurationInt(3)
        |var q2 = new DurationInt(4)
        |
        |def f = 11
        |var _ = 5
        |var v1 = 6
        |var v2 = v1 + f
        |v2 = v1
        |
        |class A
        |trait B
        |object B
        |
        |enum ListEnum[+A] {
        |  case Cons(h: A, t: ListEnum[A])
        |  case Empty
        |}
        |
        |println(ListEnum.Empty)
        |println(ListEnum.Cons(42, ListEnum.Empty))""".stripMargin
    val after =
      """
        |
        |
        |
        |List(1, 2, 3)
        |1
        |
        |
        |val res0: Int = 23
        |val res1: String = str
        |
        |def foo: String
        |def foo0: Int
        |def foo1(): Int
        |def foo2: Int
        |def foo3(): Int
        |def foo4(p: String): Int
        |def foo5(p: String): Int
        |def foo6(p: String, q: Short): Int
        |def foo7[T] => Int
        |def foo8[T](): Int
        |def foo9[T] => Int
        |def foo10[T](): Int
        |def foo11[T](p: String): Int
        |def foo12[T](p: String): Int
        |def foo13[T](p: String, q: Short): Int
        |
        |
        |val x: Int = 2
        |val y: String = 21231
        |val x2: java.io.PrintStream = null
        |val q1: scala.concurrent.duration.package.DurationInt = 3
        |var q2: scala.concurrent.duration.package.DurationInt = 4
        |
        |def f: Int
        |
        |var v1: Int = 6
        |var v2: Int = 17
        |v2: Int = 6
        |
        |// defined class A
        |// defined trait B
        |// defined object B
        |
        |// defined class ListEnum
        |
        |
        |
        |
        |Empty
        |Cons(42,Empty)""".stripMargin
    doRenderTest(before, after)
  }

  def testBracelessSyntax(): Unit = {
    val before =
      """def foo42(x: Int) =
        |  val y = x + 1
        |  y + 1
        |
        |class A(x: Int):
        |  val a = x + 2
        |  def method =
        |    val b = a + 2
        |    b
        |
        |foo42(1)
        |
        |A(1).method
        |""".stripMargin
    val after =
      s"""def foo42(x: Int): Int
         |
         |
         |
         |// defined class A
         |
         |
         |
         |
         |
         |val res0: Int = 3
         |
         |val res1: Int = 5""".stripMargin
    doRenderTest(before, after)
  }

  def testSealedTraitHierarchy_1(): Unit = {
    return // TODO: fix after this is fixed: https://github.com/lampepfl/dotty/issues/8677
    val editor = doRenderTest(
      """sealed trait T""",
      """// trait T"""
    )
    assertLastLine(editor, 0)
  }

  def testSealedTraitHierarchy_2(): Unit = {
    return // TODO: same as above
    val editor = doRenderTest(
      """sealed trait T
        |case class A() extends T""".stripMargin,
      """// trait T
        |// class A""".stripMargin
    )
    assertLastLine(editor, 2)
  }

  def testSealedTraitHierarchy_3(): Unit = {
    return // TODO: same as above
    val editor = doRenderTest(
      """sealed trait T
        |case class A() extends T
        |case class B() extends T""".stripMargin,
      """// trait T
        |// case class A
        |// case class B""".stripMargin
    )
    assertLastLine(editor, 2)
  }

  def testSealedTraitHierarchy_WithSpacesAndComments(): Unit = {
    return // TODO: same as above
    val editor = doRenderTest(
      """sealed trait T
        |case class A() extends T
        |case class B() extends T
        |
        |//
        |//
        |case class C() extends T
        |
        |
        |/**
        |  *
        |  */
        |case class D() extends T""".stripMargin,
      """// trait T
        |// case class A
        |// case class B
        |
        |
        |
        |// case class C
        |
        |
        |
        |
        |
        |// case class D""".stripMargin
    )
    assertLastLine(editor, 12)
  }

  def testSealedTraitHierarchy_Several(): Unit = {
    return // TODO: same as above
    val editor = doRenderTest(
      """sealed trait T1
        |
        |val x = 1
        |
        |sealed trait T2
        |case class A() extends T2
        |case class B() extends T2
        |
        |sealed trait T3
        |case class C() extends T3""".stripMargin,
      """// trait T1
        |
        |val x: Int = 1
        |
        |// trait T2
        |// case class A
        |// case class B
        |
        |// trait T3
        |// case class C""".stripMargin
    )
    assertLastLine(editor, 9)
  }

  private def assertLastLine(editor: Editor, line: Int): Unit = {
    val printer = worksheetCache.getPrinter(editor).get.asInstanceOf[WorksheetEditorPrinterRepl]
    assertEquals(
      "last processed line should point to last successfully evaluated line",
      Some(line), printer.lastProcessedLine
    )
  }
}
