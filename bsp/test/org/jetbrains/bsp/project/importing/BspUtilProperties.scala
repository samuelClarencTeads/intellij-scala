package org.jetbrains.bsp.project.importing

import ch.epfl.scala.bsp.testkit.gen.UtilGenerators.*
import org.jetbrains.bsp.BspUtil.*
import org.jetbrains.plugins.scala.SlowTests
import org.junit.experimental.categories.Category
import org.junit.{Ignore, Test}
import org.scalacheck.Prop.forAll
import org.scalatestplus.junit.AssertionsForJUnit
import org.scalatestplus.scalacheck.Checkers

@Category(Array(classOf[SlowTests]))
class BspUtilProperties extends AssertionsForJUnit with Checkers {

  @Test
  def stringOpsToUri(): Unit = check(
    forAll(genUri) { uri =>
      uri.toURI.toString == uri
    }
  )

  @Test @Ignore
  def uriOpsToFile(): Unit = check(
    forAll(genPath) { path =>
      path.toUri.toFile == path.toFile
    }
  )
}
