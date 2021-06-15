package org.jetbrains.plugins.scala.util.matchers

import org.hamcrest.Matcher

import scala.math.Ordering.Implicits.*

trait HamcrestMatchers {

  def hasSize(size: Int): Matcher[Iterable[?]] = new ScalaBaseMatcher[Iterable[?]] {
    override protected def valueMatches(actualValue: Iterable[?]): Boolean =
      actualValue.size == size

    override protected def description: String =
      s"collection of size $size"
  }

  def emptyCollection: Matcher[Iterable[?]] =
    hasSize(0)

  /**
   * Checks if actual value is greater than specified
   */
  def greaterThan[V: Ordering](value: V): Matcher[V] = new ScalaBaseMatcher[V] {
    override protected def valueMatches(actualValue: V): Boolean =
      actualValue > value

    override protected def description: String =
      s"greater than $value"
  }

  /**
   * Checks if actual value is less than specified
   */
  def lessThan[V: Ordering](value: V): Matcher[V] = new ScalaBaseMatcher[V] {

    override protected def valueMatches(actualValue: V): Boolean =
      actualValue < value

    override protected def description: String =
      s"less than $value"
  }

  /**
   * Checks if every map value satisfies to corresponding matcher.
   */
  def everyValue[K, V](matchers: Map[K, Matcher[V]]): Matcher[Map[K, V]] = new ScalaBaseMatcher[Map[K, V]] {
    override protected def valueMatches(actualValue: Map[K, V]): Boolean = {
      val keys = actualValue.keySet | matchers.keySet
      keys.forall { key =>
        (actualValue.get(key), matchers.get(key)) match {
          case (Some(actual), Some(matcher)) => matcher.matches(actual)
          case _ => false
        }
      }
    }

    override protected def description: String =
      matchers.toString
  }

  def everyValueGreaterThanIn[K, V: Ordering](value: Map[K, V]): Matcher[Map[K, V]] =
    everyValue(value.view.mapValues(greaterThan(_)).toMap)
}

object HamcrestMatchers extends HamcrestMatchers
