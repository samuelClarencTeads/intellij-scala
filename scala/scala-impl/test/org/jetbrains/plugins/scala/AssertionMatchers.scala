package org.jetbrains.plugins.scala

import org.junit.Assert.*

// TODO: move to util.assertions package
trait AssertionMatchers {

  implicit class AssertMatchersExt[T](val actual: T) {
    def shouldBe(expected: T): Unit = (actual, expected) match {
      case (actual: Double, expected: Double) => assertEquals(expected, actual, 0.01)
      case (actual: Float, expected: Float)   => assertEquals(expected, actual, 0.01)
      case (actual, expected)                 => assertEquals(expected, actual)
    }

    def shouldNotBe(notExpected: T): Unit = (actual, notExpected) match {
      case (actual: Double, notExpected: Double) => assertNotEquals(notExpected, actual, 0.01)
      case (actual: Float, notExpected: Float)   => assertNotEquals(notExpected, actual, 0.01)
      case (actual, notExpected)                 => assertNotEquals(notExpected, actual)
    }
  }
}

object AssertionMatchers extends AssertionMatchers