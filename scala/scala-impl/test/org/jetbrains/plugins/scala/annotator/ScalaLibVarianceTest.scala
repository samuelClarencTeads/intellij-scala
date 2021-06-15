package org.jetbrains.plugins.scala
package annotator

import com.intellij.psi.PsiElement

class ScalaLibVarianceTest extends VarianceTestBase {

  override def annotateFun(element: PsiElement, annotator: ScalaAnnotator, mock: AnnotatorHolderMock): Unit =
    annotator.annotate(element)(mock)

  protected def code(insertLine: String): String =
    s"""
       |object O {
       |  trait C[-T]
       |  trait Tr[+T] {
       |    $insertLine
       |  }
       |}
    """.stripMargin

  def testSCL13235(): Unit = assertNothing(messages(code("val foo: C[_ <: T]")))

  def testSCL13235_1(): Unit = assertNothing(messages(code("def foo: C[_ <: T]")))

  def testSCL13235_2(): Unit = assertMatches(messages(code("object Inner extends C[T]"))) {
      case Error("C[T]", ContravariantPosition()) :: Nil =>
    }

  def testSCL13235_3(): Unit = assertMatches(messages(code("trait Inner extends C[T]"))) {
      case Error("C[T]", ContravariantPosition()) :: Nil =>
    }

  def testSCL13235_4(): Unit = assertMatches(messages(code("class Inner extends C[T]"))) {
      case Error("C[T]", ContravariantPosition()) :: Nil =>
    }

  def testSCL13235_5(): Unit = assertMatches(messages(code("val foo: C[T]"))) {
    case Error("foo", ContravariantPosition()) :: Nil =>
  }

  def testSCL13235_6(): Unit = assertNothing(messages(
    """
      |object O {
      |trait T1
      |trait T2 extends T1
      |
      |class T[C] {
      |  def bar1[B >: T1 <: T2] = {}
      |  def bar2[B >: T2 <: T1] = {}
      |}
      |}
    """.stripMargin))

  def testSCL13236(): Unit = assertNothing(messages(
    """
      |object O {
      |  class B[T]
      |  class A[+T] {
      |    private val foo: Any = new B[T]
      |  }
      |}
    """.stripMargin))

  def testSCL13235_7(): Unit = assertNothing(messages(
    """
      |object O {
      |  trait V[T]
      |  trait V1[-T]
      |  trait V2[+T]
      |  trait MyTrait[+T, -T2] {
      |    val foo: V[? <: T]
      |    val bar: V[? >: T2]
      |    val foo1: V1[? <: T]
      |    val bar1: V1[? >: T2]
      |    val foo2: V2[? <: T]
      |    val bar2: V2[? >: T2]
      |  }
      |}
    """.stripMargin))

  def testSCL4391(): Unit = assertMatches(messages("class Thing[+A](var thing: A)")) {
      case Error("thing", ContravariantPosition()) :: Nil =>
    }

  def testSCL4391_1(): Unit = assertNothing(messages("class Thing[+A](val thing: A)"))

  //
  def testCovariantFuncClassParam(): Unit = {
    assertMatches(messages("class AA[+T](val f: Fun1[T, Int])")) {
      case Error("f", ContravariantPosition()) :: Nil =>
    }
  }

  def testCovariantFuncCaseClassParam(): Unit = {
    assertMatches(messages("case class BB[+T](f: Fun1[T, Int])")) {
      case Error("f", ContravariantPosition()) :: Nil =>
    }
  }

  def testContraVariantValClassParam(): Unit = {
    assertMatches(messages("class CC[-T](val t: T)")) {
      case Error("t", CovariantPosition()) :: Nil =>
    }
  }

  def testContraVariantCaseClassParam(): Unit = {
    assertMatches(messages("case class DD[-T](t: T)")) {
      case Error("t", CovariantPosition()) :: Nil =>
    }
  }

  def testContravariantFuncClassParam(): Unit = {
    assertMatches(messages("class EE[-T](val f: Fun1[Int, T])")) {
      case Error("f", CovariantPosition()) :: Nil =>
    }
  }

  def testContravariantFuncCaseClassParam(): Unit = {
    assertMatches(messages("case class FF[-T](f: Fun1[Int, T])")) {
      case Error("f", CovariantPosition()) :: Nil =>
    }
  }
}


