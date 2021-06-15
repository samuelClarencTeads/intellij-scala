package org.jetbrains.plugins.scala
package codeInsight
package hints

import com.intellij.codeInsight.hints.Option

class InlayParameterHintsTest extends InlayHintsTestBase {

  import Hint.{End as E, Start as S}
  import ScalaInlayParameterHintsProvider.*

  def testNoDefaultPackageHint(): Unit = doTest(
    s"""  println(42)
       |
       |  Some(42)
       |
       |  Option(null)
       |
       |  identity[Int].apply(42)
       |
       |  val pf: PartialFunction[Int, Int] = {
       |    case 42 => 42
       |  }
       |  pf.applyOrElse(42, identity[Int])
       |
       |  Seq(1, 2, 3).collect(pf)""".stripMargin
  )

  def testParameterHint(): Unit = doTest(
    s"""  def foo(foo: Int, otherFoo: Int = 42)
       |         (bar: Int)
       |         (baz: Int = 0): Unit = {}
       |
       |  foo(${S}foo =${E}42, ${S}otherFoo =${E}42)(${S}bar =${E}42)()
       |  foo(${S}foo =${E}42)(bar = 42)()
       |  foo(${S}foo =${E}42, ${S}otherFoo =${E}42)(${S}bar =${E}42)(${S}baz =${E}42)""".stripMargin
  )

  def testConstructorParameterHint(): Unit = doTest(
    s"""  new Bar(${S}bar =${E}42)
       |  new Bar()
       |
       |  class Bar(bar: Int = 42)
       |
       |  new Baz()
       |
       |  class Baz""".stripMargin
  )

  def testNoInfixExpressionHint(): Unit = doTest(
    s"""  def foo(foo: Int): Unit = {}
       |
       |  foo(${S}foo =${E}this foo 42)""".stripMargin
  )

  def testNoTrivialHint(): Unit = doTest(
    s"""  def foo(bar: String): Unit = {}
       |  def foo(length: Int): Unit = {}
       |  def bar(hashCode: Int): Unit = {}
       |  def bar(classOf: Class[_]): Unit = {}
       |  def bar(baz: () => Unit): Unit = {}
       |
       |  val bar = ""
       |
       |  def bazImpl(): Unit = {}
       |
       |  foo(${S}bar =${E}null)
       |  foo(bar)
       |  foo(bar.length)
       |  bar(bar.hashCode())
       |  bar(classOf[String])
       |  baz(bazImpl())""".stripMargin
  )

  def testVarargHint(): Unit = doTest(
    s"""  def foo(foo: Int, bars: Int*): Unit = {}
       |
       |  foo(${S}foo =${E}42)
       |  foo(${S}foo =${E}42, bars = 42, 42 + 0)
       |  foo(foo = 42)
       |  foo(foo = 42, ${S}bars =${E}42, 42 + 0)
       |  foo(${S}foo =${E}42, ${S}bars =${E}42, 42 + 0)
       |  foo(foo = 42, bars = 42, 42 + 0)""".stripMargin
  )

  def testVarargConstructorHint(): Unit = doTest(
    s"""  new Foo(${S}foo =${E}42)
       |  new Foo(${S}foo =${E}42, bars = 42, 42 + 0)
       |  new Foo(foo = 42)
       |  new Foo(foo = 42, ${S}bars =${E}42, 42 + 0)
       |  new Foo(${S}foo =${E}42, ${S}bars =${E}42, 42 + 0)
       |  new Foo(foo = 42, bars = 42, 42 + 0)
       |
       |  class Foo(foo: Int, bars: Int*)""".stripMargin
  )

  def testNoSyntheticParameterHint(): Unit = doTest(
    s"""  def foo: Int => Int = identity
       |
       |  foo(42)
       |  foo.apply(42)""".stripMargin
  )

  def testSingleCharacterParameterHint(): Unit = doTest(
    s"""  def foo(f: Int): Unit = {}
       |
       |  foo(42)
     """.stripMargin
  )

  def testNoFunctionalParameterHint(): Unit = doTest(
    s"""  def foo(pf: PartialFunction[Int, Int]): Unit = {
       |    pf(42)
       |    pf.apply(42)
       |  }
       |
       |  foo {
       |    case 42 => 42
       |  }
       |
       |  def bar(bar: Int = 42)
       |         (collector: PartialFunction[Int, Int]): Unit = {
       |    pf(bar)
       |    pf.apply(bar)
       |
       |    foo(collector)
       |    foo({ case 42 => 42 })
       |  }
       |
       |  bar(${S}bar =${E}0) {
       |    case 42 => 42
       |  }""".stripMargin
  )

  def testJavaParameterHint(): Unit = {
    this.configureJavaFile(
      fileText =
        """public class Bar {
          |  public static void bar(int bar) {}
          |}""".stripMargin,
      className = "Bar.java"
    )
    doTest(s"  Bar.bar(${S}bar =${E}42)")
  }

  def testJavaConstructorParameterHint(): Unit = {
    this.configureJavaFile(
      fileText =
        """public class Bar {
          |  public Bar(int bar) {}
          |}""".stripMargin,
      className = "Bar.java"
    )
    doTest(s"  new Bar(${S}bar =${E}42)")
  }

  def testVarargJavaConstructorHint(): Unit = {
    this.configureJavaFile(
      fileText =
        """public class Bar {
          |  public Bar(int foo, int... bars) {}
          |}""".stripMargin,
      className = "Bar.java"
    )
    doTest(
      s"""  new Bar(${S}foo =${E}42)
         |  new Bar(${S}foo =${E}42, bars = 42, 42 + 0)
         |  new Bar(foo = 42)
         |  new Bar(foo = 42, ${S}bars =${E}42, 42 + 0)
         |  new Bar(${S}foo =${E}42, ${S}bars =${E}42, 42 + 0)
         |  new Bar(foo = 42, bars = 42, 42 + 0)""".stripMargin
    )
  }

  def testNoApplyUpdateParameterHints(): Unit = doTest(
    s"""  private val array: Array[Double] = Array.emptyDoubleArray
       |
       |  def apply(index: Int): Double = array(index)
       |
       |  this(0)
       |  this.apply(0)
       |
       |  def update(index: Int, value: Double): Unit = {
       |    array(index) = value
       |  }
       |
       |  this(0) = 0d
       |  this.update(0, 0d)
       |
       |  Seq(1, 2, 3)
       |  Seq.apply(1, 2, 3)""".stripMargin
  )

  def testApplyUpdateParameterHints(): Unit = doTest(
    s"""  private val array: Array[Double] = Array.emptyDoubleArray
       |
       |  def apply(index: Int): Double = array(index)
       |
       |  this(${S}index =${E}0)
       |  this.apply(${S}index =${E}0)
       |
       |  def update(index: Int, value: Double): Unit = {
       |    array(index) = value
       |  }
       |
       |  this(${S}index =${E}0) = 0d
       |  this.update(${S}index =${E}0, ${S}value =${E}0d)
       |
       |  Seq(1, 2, 3)
       |  Seq.apply(1, 2, 3)""".stripMargin,
    option = applyUpdateParameterNames
  )

  def testNonLiteralArgumentParameterHint(): Unit = doTest(
    s"""  def foo(length: Int): Unit = {}
       |  foo("".length())
       |
       |  def bar(hashCode: Int): Unit = {}
       |  bar("".hashCode())""".stripMargin,
    option = referenceParameterNames
  )

  def testNoParameterHintsByCamelCase(): Unit = doTest(
    s"""  type Type = String
       |
       |  val `type`: Type = "type"
       |  def  getType: Type = "type"
       |  def withType(`type`: Type): Unit = {}
       |
       |  withType(`type`)
       |  withType(getType)
       |  withType($S`type` =$E"type")""".stripMargin,
    option = referenceParameterNames
  )

  private def doTest(text: String, option: Option = null): Unit = {
    def setOption(default: Boolean): Unit = option match {
      case null =>
      case _ =>
        val defaultValue = option.getDefaultValue
        option.set(if (default) defaultValue else !defaultValue)
    }

    try {
      setOption(false)
      configureFromFileText(text)
      getFixture.testInlays()
    } finally {
      setOption(true)
    }
  }
}
