package org.jetbrains.plugins.scala.annotator

import junit.framework.TestCase
import org.jetbrains.plugins.scala.annotator.Tree.*
import org.jetbrains.plugins.scala.annotator.TypeDiff.*
import org.junit.Assert.assertEquals

class TreeTest
  extends TestCase {

  import fastparse.*
  import NoWhitespace.*

  private def letterOrDigit[_: P]: P[Unit] = P {
    CharPred(_.isLetterOrDigit)
  }

  private def comma[_: P]: P[Unit] =
    P(", ").rep(0)

  private def element[_: P]: P[Leaf[TypeDiff]] = P {
    letterOrDigit.rep(1).!.map(s => Leaf(Match(s)))
  }

  private def group[_: P]: P[Node[TypeDiff]] = P {
    "(" ~~ parser.rep(0) ~~ ")"
  }.map(Node(_*))

  private def parser[_: P]: P[Tree[TypeDiff]] = P {
    (group | element) ~ comma
  }

  def testFlatten(): Unit = {
    assertFlattenedTo(100, "", "")
    assertFlattenedTo(100, "foo", "foo")
    assertFlattenedTo(100, "foo, bar", "foo, bar")

    assertFlattenedTo(100, "()", "")
    assertFlattenedTo(100, "(foo)", "foo")
    assertFlattenedTo(100, "(foo, bar)", "foo, bar")
    assertFlattenedTo(100, "foo, (bar)", "foo, bar")
    assertFlattenedTo(100, "(foo), bar", "foo, bar")
    assertFlattenedTo(100, "(foo), (bar)", "foo, bar")

    assertFlattenedTo(100, "(())", "")
    assertFlattenedTo(100, "((foo))", "foo")
    assertFlattenedTo(100, "((foo, bar))", "foo, bar")
    assertFlattenedTo(100, "(foo, (bar))", "foo, bar")
    assertFlattenedTo(100, "((foo), bar)", "foo, bar")
    assertFlattenedTo(100, "((foo), (bar))", "foo, bar")
  }

  // The root is implicitly added by the assertFlattenedTo method
  def testMaxChars(): Unit = {
    assertFlattenedTo(0, "", "")
    assertFlattenedTo(0, "foo", "(foo)")
    assertFlattenedTo(0, "foo, bar", "(foo, bar)")

    assertFlattenedTo(0, "()", "")
    assertFlattenedTo(0, "(foo)", "(foo)")
    assertFlattenedTo(0, "foo, (bar)", "(foo, (bar))")
    assertFlattenedTo(0, "(foo), bar", "((foo), bar)")
    assertFlattenedTo(0, "(foo), (bar)", "(foo), (bar)")

    assertFlattenedTo(3, "foo, (bar)", "foo, (bar)")
    assertFlattenedTo(3, "(foo), bar", "(foo), bar")
    assertFlattenedTo(3, "(foo), (bar)", "foo, (bar)")

    assertFlattenedTo(0, "(foo), (bar), (moo)", "(foo), (bar), (moo)")
    assertFlattenedTo(3, "(foo), (bar), (moo)", "foo, (bar), (moo)")
    assertFlattenedTo(6, "(foo), (bar), (moo)", "foo, bar, (moo)")
    assertFlattenedTo(9, "(foo), (bar), (moo)", "foo, bar, moo")

    assertFlattenedTo(0, "foo, (bar, (moo))", "(foo, (bar, (moo)))")
    assertFlattenedTo(3, "foo, (bar, (moo))", "foo, (bar, (moo))")
    assertFlattenedTo(6, "foo, (bar, (moo))", "foo, bar, (moo)")
    assertFlattenedTo(9, "foo, (bar, (moo))", "foo, bar, moo")

    assertFlattenedTo(0, "((foo), bar), moo", "(((foo), bar), moo)")
    assertFlattenedTo(3, "((foo), bar), moo", "((foo), bar), moo")
    assertFlattenedTo(6, "((foo), bar), moo", "(foo), bar, moo")
    assertFlattenedTo(9, "((foo), bar), moo", "foo, bar, moo")
  }

  def testGroupLength(): Unit = {
    assertFlattenedTo(0, "foo", "foo", 6)
    assertFlattenedTo(0, "foo", "foo", 3)

    assertFlattenedTo(3, "foo, (bar)", "(foo, (bar))", 1)
  }

  private def assertFlattenedTo(maxChars: Int, elements: String, expectedElements: String, nodeLength: Int = 0): Unit = {
    val result = fastparse.parse("(" + elements + ")", group(_)).get.value.flattenTo(lengthOf(nodeLength), maxChars)
//    val result = group.parse("(" + elements + ")").get.value.flattenTo(lengthOf(nodeLength), maxChars)
    assertEquals(expectedElements, result.map(asString).mkString(", "))
  }

  private def asString(diff: Tree[TypeDiff]): String = diff match {
    case Node(elements*) => s"(${elements.map(asString).mkString(", ")})"
    case Leaf(Match(text, _)) => text
    case Leaf(Mismatch(text, _)) => s"~$text~"
  }
}
