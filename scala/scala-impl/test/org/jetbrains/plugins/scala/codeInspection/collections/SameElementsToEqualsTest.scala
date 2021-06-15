package org.jetbrains.plugins.scala
package codeInspection
package collections

import com.intellij.testFramework.EditorTestUtil.{SELECTION_END_TAG as END, SELECTION_START_TAG as START}

/**
 * @author Nikolay.Tropin
 */
abstract class SameElementsToEqualsInspectionTest extends OperationsOnCollectionInspectionTest {

  override protected val classOfInspection: Class[? <: OperationOnCollectionInspection] =
    classOf[SameElementsToEqualsInspection]
}

class SameElementsToEqualsTest extends SameElementsToEqualsInspectionTest {

  override protected val hint: String = ScalaInspectionBundle.message("replace.sameElements.with.equals")

  def testSeqs(): Unit = {
    doTest(
      s"Vector(1) ${START}sameElements$END Seq(1)",
      "Vector(1) sameElements Seq(1)",
      "Vector(1) == Seq(1)"
    )
  }

  def testSets(): Unit = {
    doTest(
      s"Set(1) ${START}sameElements$END scala.collection.immutable.BitSet(1)",
      "Set(1) sameElements scala.collection.immutable.BitSet(1)",
      "Set(1) == scala.collection.immutable.BitSet(1)"
    )
  }

  def testMaps(): Unit = {
    doTest(
      s"Map(1 -> 1).${START}sameElements$END(scala.collection.mutable.Map(1 -> 1))",
      "Map(1 -> 1).sameElements(scala.collection.mutable.Map(1 -> 1))",
      "Map(1 -> 1) == scala.collection.mutable.Map(1 -> 1)"
    )
  }

  def testSeqSet(): Unit = {
    checkTextHasNoErrors("Seq(1) sameElements Set(1)")
  }

  def testSortedSets(): Unit = {
    checkTextHasNoErrors("scala.collection.immutable.BitSet(1) sameElements scala.collection.mutable.SortedSet(1)")
  }

  def testSortedMaps(): Unit = {
    checkTextHasNoErrors("scala.collection.immutable.TreeMap(1 -> 1) sameElements scala.collection.immutable.TreeMap(1 -> 1)")
  }

}

class CorrespondsToEqualsTest extends SameElementsToEqualsInspectionTest {

  override protected val hint: String =
    ScalaInspectionBundle.message("replace.corresponds.with.equals")

  def test1(): Unit = {
    doTest(
      s"Vector(1).${START}corresponds$END(Seq(1))((x, y) => x == y)",
      "Vector(1).corresponds(Seq(1))((x, y) => x == y)",
      "Vector(1) == Seq(1)"
    )
  }

  def test2(): Unit = {
    doTest(
      s"Vector(1).${START}corresponds$END(Seq(1))(_ == _)",
      "Vector(1).corresponds(Seq(1))(_ == _)",
      "Vector(1) == Seq(1)"
    )
  }

  def test3(): Unit = {
    checkTextHasNoErrors("Vector(1).corresponds(Seq(1))((x, y) => x > y)")
  }
}