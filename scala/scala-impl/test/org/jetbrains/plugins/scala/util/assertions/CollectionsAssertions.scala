package org.jetbrains.plugins.scala.util.assertions

import org.junit.ComparisonFailure

import scala.language.higherKinds

trait CollectionsAssertions {

  def assertCollectionEquals[T, C[_] <: Iterable[?]](expected: C[T], actual: C[T]): Unit =
    assertCollectionEquals("", expected, actual)

  def assertCollectionEquals[T, C[_] <: Iterable[?]](message: String, expected: C[T], actual: C[T]): Unit =
    if (expected != actual)
      throw new ComparisonFailure(message, expected.mkString("\n"), actual.mkString("\n"))
}

object CollectionsAssertions extends CollectionsAssertions