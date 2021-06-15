package org.jetbrains.plugins.scala
package debugger.evaluateExpression

import org.jetbrains.plugins.scala.debugger.*
import org.junit.experimental.categories.Category

/**
 * Nikolay.Tropin
 * 8/5/13
 */
@Category(Array(classOf[DebuggerTests]))
class VariablesFromPatternsEvaluationTest_211 extends VariablesFromPatternsEvaluationTestBase {
  override protected def supportedIn(version: ScalaVersion) = version  == LatestScalaVersions.Scala_2_11
}
@Category(Array(classOf[DebuggerTests]))
class VariablesFromPatternsEvaluationTest_212 extends VariablesFromPatternsEvaluationTestBase {
  override protected def supportedIn(version: ScalaVersion) =
    version >= LatestScalaVersions.Scala_2_12 && version <= LatestScalaVersions.Scala_2_13
}
@Category(Array(classOf[DebuggerTests]))
class VariablesFromPatternsEvaluationTest_3_0 extends VariablesFromPatternsEvaluationTestBase {
  override protected def supportedIn(version: ScalaVersion) = version  >= LatestScalaVersions.Scala_3_0

  override def testAnonymousInMatch(): Unit = failing(super.testAnonymousInMatch())
}

@Category(Array(classOf[DebuggerTests]))
abstract class VariablesFromPatternsEvaluationTestBase extends ScalaDebuggerTestCase{
  addFileWithBreakpoints("Match.scala",
    s"""
       |object Match {
       |  val name = "name"
       |  def main(args: Array[String]): Unit = {
       |    val x = (List(1, 2), Some("z"), None)
       |    x match {
       |      case all @ (list @ List(q, w), some @ Some(z), _) =>
       |        println()$bp
       |      case _ =>
       |    }
       |  }
       |}
      """.stripMargin.trim()
  )
  def testMatch(): Unit = {
    runDebugger() {
      waitForBreakpoint()
      evalEquals("all", "(List(1, 2),Some(z),None)")
      evalEquals("list", "List(1, 2)")
      evalEquals("x", "(List(1, 2),Some(z),None)")
      evalEquals("name", "name")
      evalEquals("q", "1")
      evalEquals("z", "z")
      evalEquals("some", "Some(z)")
      evalEquals("args", "[]")
    }
  }

  addFileWithBreakpoints("MatchInForStmt.scala",
    s"""
       |object MatchInForStmt {
       |  val name = "name"
       |  def main(args: Array[String]): Unit = {
       |    for (s <- List("a", "b"); if s == "a"; ss = s + s; i <- List(1,2); if i == 1; si = s + i) {
       |      val x = (List(1, 2), Some("z"), ss :: i :: Nil)
       |      x match {
       |        case all @ (q :: qs, some @ Some(z), list @ List(m, _)) =>
       |          println()$bp
       |        case _ =>
       |      }
       |    }
       |  }
       |}
      """.stripMargin.trim()
  )
  def testMatchInForStmt(): Unit = {
    runDebugger() {
      waitForBreakpoint()
      evalEquals("all", "(List(1, 2),Some(z),List(aa, 1))")
      evalEquals("x", "(List(1, 2),Some(z),List(aa, 1))")
      evalEquals("name", "name")
      evalEquals("q", "1")
      evalEquals("qs", "List(2)")
      evalEquals("z", "z")
      evalEquals("list", "List(aa, 1)")
      evalEquals("m", "aa")
      evalEquals("some", "Some(z)")
      evalEquals("ss", "aa")
      evalEquals("i", "1")
      evalEquals("args", "[]")
    }
  }

  addFileWithBreakpoints("RegexMatch.scala",
    {
      val pattern = """"(-)?(\\d+)(\\.\\d*)?".r"""
      s"""
         |object RegexMatch {
         |  val name = "name"
         |  def main(args: Array[String]): Unit = {
         |    val Decimal = $pattern
         |    "-2.5" match {
         |      case number @ Decimal(sign, _, dec) =>
         |        println()$bp
         |      case _ =>
         |    }
         |  }
         |}
      """.stripMargin.trim()
    }

  )
  def testRegexMatch(): Unit = {
    runDebugger() {
      waitForBreakpoint()
      evalEquals("number", "-2.5")
      evalEquals("sign", "-")
      evalEquals("dec", ".5")
      evalEquals("name", "name")
    }
  }

  addFileWithBreakpoints("Multilevel.scala",
    s"""
       |object Multilevel {
       |  val name = "name"
       |  def main(args: Array[String]): Unit = {
       |    List(None, Some(1 :: 2 :: Nil)) match {
       |      case List(none, some) =>
       |        some match {
       |          case Some(seq) =>
       |            seq match {
       |              case Seq(1, two) =>
       |                println()$bp
       |              case _ =>
       |            }
       |          case _ =>
       |        }
       |      case _ =>
       |    }
       |  }
       |}
      """.stripMargin.trim()
  )
  def testMultilevel(): Unit = {
    runDebugger() {
      waitForBreakpoint()
      evalEquals("name", "name")
      evalEquals("args", "[]")
      evalEquals("none", "None")
      evalEquals("some", "Some(List(1, 2))")
      evalEquals("seq", "List(1, 2)")
      evalEquals("two", "2")
    }
  }

  addFileWithBreakpoints("LocalInMatch.scala",
    s"""
       |object LocalInMatch {
       |  val name = "name"
       |  def main(args: Array[String]): Unit = {
       |    Option("a") match {
       |      case None =>
       |      case some @ Some(a) =>
       |        def foo(i: Int): Unit = {
       |          println()$bp
       |        }
       |        foo(10)
       |    }
       |  }
       |}
      """.stripMargin.trim()
  )
  def testLocalInMatch(): Unit = {
    runDebugger() {
      waitForBreakpoint()
      evalEquals("name", "name")
      evalEquals("args", "[]")
      evalEquals("some", "Some(a)")
      evalEquals("a", "a")
      evalEquals("i", "10")
    }
  }

  addFileWithBreakpoints("AnonymousInMatch.scala",
    s"""
       |object AnonymousInMatch {
       |  val name = "name"
       |  def main(args: Array[String]): Unit = {
       |    Option("a") match {
       |      case None =>
       |      case some @ Some(a) =>
       |        List(10) foreach { i =>
       |          println()$bp
       |        }
       |    }
       |  }
       |}
      """.stripMargin.trim()
  )
  def testAnonymousInMatch(): Unit = {
    runDebugger() {
      waitForBreakpoint()
      evalEquals("name", "name")
      evalEquals("args", "[]")
      evalEquals("some", "Some(a)")
      evalEquals("a", "a")
      evalEquals("i", "10")
    }
  }

}
