package org.jetbrains.plugins.scala
package codeInspection
package collections

import com.intellij.testFramework.EditorTestUtil.{SELECTION_END_TAG as END, SELECTION_START_TAG as START}

/**
  * Nikolay.Tropin
  * 5/30/13
  */
abstract class MapGetOrElseBooleanTest extends OperationsOnCollectionInspectionTest {

  override protected val classOfInspection: Class[? <: OperationOnCollectionInspection] =
    classOf[MapGetOrElseBooleanInspection]
}

class ReplaceWithExistsTest extends MapGetOrElseBooleanTest {

  override protected val hint: String =
    ScalaInspectionBundle.message("map.getOrElse.false.hint")

  def test_1(): Unit = {
    val selected = s"None.${START}map(x => true).getOrElse(false)$END"
    checkTextHasError(selected)

    val text = "None.map(x => true).getOrElse(false)"
    val result = "None.exists(x => true)"
    testQuickFix(text, result, hint)
  }

  "aaa ".split(' ')

  def test_2(): Unit = {
    val selected =
      s"""class Test {
         |  Some(0) ${START}map (_ => true) getOrElse false$END
         |}""".stripMargin
    checkTextHasError(selected)

    val text =
      """class Test {
        |  Some(0) map (_ => true) getOrElse false
        |}""".stripMargin
    val result =
      """class Test {
        |  Some(0) exists (_ => true)
        |}""".stripMargin
    testQuickFix(text, result, hint)
  }

  def test_3(): Unit = {
    val selected =
      s"""val valueIsGoodEnough: (Any) => Boolean = _ => true
         |(None ${START}map valueIsGoodEnough).getOrElse(false)$END""".stripMargin
    checkTextHasError(selected)
    val text =
      """val valueIsGoodEnough: (Any) => Boolean = _ => true
        |(None map valueIsGoodEnough).getOrElse(false)""".stripMargin
    val result =
      """val valueIsGoodEnough: (Any) => Boolean = _ => true
        |None exists valueIsGoodEnough""".stripMargin
    testQuickFix(text, result, hint)
  }
}

class ReplaceWithForallTest extends MapGetOrElseBooleanTest {

  override protected val hint: String =
    ScalaInspectionBundle.message("map.getOrElse.true.hint")

  def test(): Unit = {
    val selected =
      s"""val valueIsGoodEnough: (Any) => Boolean = _ => true
         |(None ${START}map valueIsGoodEnough).getOrElse(true)$END""".stripMargin
    checkTextHasError(selected)
    val text =
      """val valueIsGoodEnough: (Any) => Boolean = _ => true
        |(None map valueIsGoodEnough).getOrElse(true)""".stripMargin
    val result =
      """val valueIsGoodEnough: (Any) => Boolean = _ => true
        |None forall valueIsGoodEnough""".stripMargin
    testQuickFix(text, result, hint)
  }
}
