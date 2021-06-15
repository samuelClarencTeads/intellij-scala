package org.jetbrains.plugins.scala
package codeInsight
package intention
package controlFlow

import com.intellij.testFramework.EditorTestUtil

/**
  * User: Nikolay.Tropin
  * Date: 4/17/13
  */
class ReplaceWhileWithDoWhileIntentionTest extends intentions.ScalaIntentionTestBase {

  import EditorTestUtil.{CARET_TAG as CARET}

  override def familyName = ScalaCodeInsightBundle.message("family.name.replace.while.with.do.while")

  def testReplaceWhile1(): Unit = {
    val text =
      s"""
         |class X {
         |  val flag: Boolean
         |
         |  def f {
         |    ${CARET}while(flag) {
         |      //looping
         |    }
         |  }
         |}""".stripMargin
    val resultText =
      s"""class X {
         |  val flag: Boolean
         |
         |  def f {
         |    if (flag) {
         |      do {
         |        //looping
         |      } while (flag)
         |    }
         |  }
         |}""".stripMargin

    doTest(text, resultText)
  }

  def testReplaceWhile2(): Unit = {
    val text =
      s"""class X {
         |  val flag: Boolean
         |
         |  def f {
         |    while$CARET(flag) {
         |      //looping
         |    }
         |  }
         |}""".stripMargin
    val resultText =
      s"""class X {
         |  val flag: Boolean
         |
         |  def f {
         |    if (flag) {
         |      do {
         |        //looping
         |      } while (flag)
         |    }
         |  }
         |}""".stripMargin

    doTest(text, resultText)
  }

  def testReplaceWhile3(): Unit = {
    val text =
      s"""class X {
         |  val flag: Boolean
         |
         |  def f {
         |    while$CARET(flag) print("")
         |  }
         |}""".stripMargin
    val resultText =
      s"""class X {
         |  val flag: Boolean
         |
         |  def f {
         |    if (flag) {
         |      do print("") while (flag)
         |    }
         |  }
         |}""".stripMargin

    doTest(text, resultText)
  }
}
