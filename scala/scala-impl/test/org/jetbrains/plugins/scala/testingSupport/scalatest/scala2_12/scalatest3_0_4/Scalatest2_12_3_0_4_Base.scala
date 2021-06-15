package org.jetbrains.plugins.scala
package testingSupport
package scalatest
package scala2_12.scalatest3_0_4

import org.jetbrains.plugins.scala.DependencyManagerBase.*
import org.jetbrains.plugins.scala.base.libraryLoaders.{IvyManagedLoader, LibraryLoader}

abstract class Scalatest2_12_3_0_4_Base extends ScalaTestTestCase {

  override protected def supportedIn(version: ScalaVersion): Boolean = version  == LatestScalaVersions.Scala_2_12

  override protected def additionalLibraries: Seq[LibraryLoader] = IvyManagedLoader(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "org.scalatest" %% "scalatest" % "3.0.4",
      "org.scalactic" %% "scalactic" % "3.0.4"
  ) :: Nil
}
