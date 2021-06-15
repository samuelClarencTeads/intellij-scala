package org.jetbrains.plugins.scala.testingSupport.utest.scala2_12.utest_0_7_4

import org.jetbrains.plugins.scala.DependencyManagerBase.*
import org.jetbrains.plugins.scala.base.libraryLoaders.{IvyManagedLoader, LibraryLoader}
import org.jetbrains.plugins.scala.testingSupport.utest.UTestTestCase
import org.jetbrains.plugins.scala.{LatestScalaVersions, ScalaVersion}

abstract class UTestTestBase_2_12_0_7_4 extends UTestTestCase {
  override protected def supportedIn(version: ScalaVersion): Boolean = version == LatestScalaVersions.Scala_2_12

  override protected def additionalLibraries: Seq[LibraryLoader] =
    IvyManagedLoader("com.lihaoyi" %% "utest" % "0.7.4") :: Nil

  override protected val testSuiteSecondPrefix = ""
}
