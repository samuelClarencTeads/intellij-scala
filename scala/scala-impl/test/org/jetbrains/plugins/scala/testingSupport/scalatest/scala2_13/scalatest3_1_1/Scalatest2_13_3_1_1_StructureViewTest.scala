package org.jetbrains.plugins.scala.testingSupport.scalatest.scala2_13.scalatest3_1_1

import org.jetbrains.plugins.scala.testingSupport.scalatest.fileStructureView.*

class Scalatest2_13_3_1_1_StructureViewTest extends Scalatest2_13_3_1_1_Base
  with FeatureSpecFileStructureViewTest
  with FlatSpecFileStructureViewTest
  with FreeSpecFileStructureViewTest
  with FunSuiteFileStructureViewTest
  with PropSpecFileStructureViewTest
  with WordSpecFileStructureViewTest
  with FunSpecFileStructureViewTest {

  override protected def feature = "Feature"
  override protected def scenario = "Scenario"
}
