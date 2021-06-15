package org.jetbrains.plugins.scala.lang.formatter.tests

import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.formatter.AbstractScalaFormatterTestBase
import org.jetbrains.plugins.scala.util.Markers

/**
  * @author Roman.Shein
  *         Date: 27.11.2015
  */
class ScalaFormatSelectionTest extends AbstractScalaFormatterTestBase with Markers {

  def testSelection(): Unit = {
    val before =
      s"""
        |class MyClass {
        |  val a: Int = 1
        |  val b: Int = 2
        |  def foo() = ${startMarker}a+b$endMarker
        |  def bar() = a+b
        |}
      """.stripMargin

    val after =
      """
        |class MyClass {
        |  val a: Int = 1
        |  val b: Int = 2
        |  def foo() = a + b
        |  def bar() = a+b
        |}
      """.stripMargin

    doTextTest(before, after)
  }

  def testSelectionInParent(): Unit = {
    val before =
      s"""
        |class MyClass {
        |  val a: Int = 1
        |  val b: Int = 2
        |  a+b
        |
        |$startMarker def foo() = a+b$endMarker
        |  def bar() = a+b
        |}
      """.stripMargin

    val after =
      """
        |class MyClass {
        |  val a: Int = 1
        |  val b: Int = 2
        |  a+b
        |
        |  def foo() = a + b
        |  def bar() = a+b
        |}
      """.stripMargin

    doTextTest(before, after)
  }

  def testSelectionNearScalaDoc(): Unit = {
    val before =
      s"""
        |class MyClass {
        |$startMarker/**
        |  * @param x
        |  * @param y
        |  * @return x+y
        |  */$endMarker
        |def foo(x: Int, y: Int): Int = x+y
        |}
      """.stripMargin

    val after =
      """
        |class MyClass {
        |  /**
        |   * @param x
        |   * @param y
        |   * @return x+y
        |   */
        |def foo(x: Int, y: Int): Int = x+y
        |}
      """.stripMargin

    doTextTest(before, after)
  }

  def testSelectionNearScalaDocExtended(): Unit = {
    val before =
      s"""
        |class MyClass {
        |$startMarker/**
        |  * @param x
        |    @param y
        |    @return x+y
        |  */$endMarker
        |def foo(x: Int, y: Int): Int = x+y
        |}
      """.stripMargin

    val after =
      """
        |class MyClass {
        |  /**
        |   * @param x
        |   * @param y
        |   * @return x+y
        |   */
        |def foo(x: Int, y: Int): Int = x+y
        |}
      """.stripMargin

    doTextTest(before, after)
  }

  def testSCL10000(): Unit = {
    val before =
      s"""
        |class Test {
        |//someComment
        |  def foo() = $startMarker{
        |}$endMarker
        |}
      """.stripMargin

    val after =
      """
        |class Test {
        |//someComment
        |  def foo() = {
        |  }
        |}
      """.stripMargin

    doTextTest(before, after)
  }

  def testSCL10000Vals(): Unit = {
    val before =
      s"""
        |class Test {
        |  def foo() = ???
        |//someComment
        |  val bar = $startMarker{
        |  42
        |}$endMarker
        |}
      """.stripMargin.replace("\r","")

    val after =
      """
        |class Test {
        |  def foo() = ???
        |//someComment
        |  val bar = {
        |    42
        |  }
        |}
      """.stripMargin

    doTextTest(before, after)
  }

//  TODO: the odds of such behavior are extremely low, and fixing the issue involves extensive change of getDummyBlocks
//  def testSCL10000WithGroupedVals(): Unit = {
//    getCommonSettings.ALIGN_GROUP_FIELD_DECLARATIONS = true
//
//    val before =
//      s"""
//        |class Test {
//        |  val longValName = 42
//        |//someComment
//        |  val short       = $startMarker{
//        |11
//        |}$endMarker
//        |}
//      """.stripMargin
//
//    val after =
//      """
//        |class Test {
//        |  val longValName = 42
//        |//someComment
//        |  val short       = {
//        |    11
//        |  }
//        |}
//      """.stripMargin
//
//    doTextTest(before, after)
//  }

  def testSCL15147_FormatMethodCallChain_1(): Unit = {
    getCommonSettings.ALIGN_MULTILINE_CHAINED_METHODS = true
    val before =
      s"""class A {
         |  myObject.method1()
         |          .method2().method3().method4()$startMarker
         |          $endMarker.method5().method6()
         |}""".stripMargin.withNormalizedSeparator

    val after =
      """class A {
        |  myObject.method1()
        |          .method2().method3().method4()
        |          .method5().method6()
        |}""".stripMargin.withNormalizedSeparator

    doTextTest(before, after)
  }

  def testSCL15147_FormatMethodCallChain_2(): Unit = {
    getCommonSettings.ALIGN_MULTILINE_CHAINED_METHODS = true
    val before =
      s"""class A {
         |  myObject.method1()
         |          .method2().method3().method4()
         |          $startMarker.method5().method6()$endMarker
         |}""".stripMargin.withNormalizedSeparator

    val after =
      """class A {
        |  myObject.method1()
        |          .method2().method3().method4()
        |          .method5().method6()
        |}""".stripMargin.withNormalizedSeparator

    doTextTest(before, after)
  }
}
