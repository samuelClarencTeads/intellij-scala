package org.jetbrains.plugins.scala.testingSupport.scalatest

import org.jetbrains.plugins.scala.testingSupport.scalatest.scopeTest.*
import org.jetbrains.plugins.scala.testingSupport.scalatest.singleTest.tagged.{FlatSpecTaggedSingleTestTest, FreeSpecTaggedSingleTestTest}
import org.jetbrains.plugins.scala.testingSupport.scalatest.singleTest.{ScalaTestSingleTestTest, SpecSingleTestTest}

trait ScalaTestSelectedTests extends ScalaTestTestCase
  with ScalaTestSelectedSingleTests
  with ScalaTestSelectedScopeTests

trait ScalaTestSelectedSingleTests extends ScalaTestTestCase
  with ScalaTestSingleTestTest
  with SpecSingleTestTest
  with FlatSpecTaggedSingleTestTest
  with FreeSpecTaggedSingleTestTest

trait ScalaTestSelectedScopeTests extends ScalaTestTestCase
  with FeatureSpecScopeTest
  with FreeSpecScopeTest
  with FunSpecScopeTest
  with WordSpecScopeTest
  with FlatSpecScopeTest
