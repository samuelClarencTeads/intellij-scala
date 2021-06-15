package org.jetbrains.plugins.scala
package testingSupport
package scalatest
package scala2_11
package scalatest2_1_7

import org.jetbrains.plugins.scala.DependencyManagerBase.*
import org.jetbrains.plugins.scala.base.libraryLoaders.{IvyManagedLoader, LibraryLoader}

abstract class Scalatest2_11_2_1_7_Base extends ScalaTestTestCase {

  override protected def supportedIn(version: ScalaVersion): Boolean = version  == LatestScalaVersions.Scala_2_11

  override protected def additionalLibraries: Seq[LibraryLoader] =
    IvyManagedLoader(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "org.scalatest" %% "scalatest" % "2.1.7",
    ) :: Nil
}
