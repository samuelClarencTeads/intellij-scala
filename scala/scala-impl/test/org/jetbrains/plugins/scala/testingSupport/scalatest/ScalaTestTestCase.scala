package org.jetbrains.plugins.scala.testingSupport.scalatest

import org.jetbrains.plugins.scala.lang.structureView.element.Test.*
import org.jetbrains.plugins.scala.testingSupport.ScalaTestingTestCase
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestConfigurationProducer
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestConfigurationProducer
import org.jetbrains.plugins.scala.testingSupport.test.structureView.TestNodeProvider
import org.junit.Assert.fail

abstract class ScalaTestTestCase extends ScalaTestingTestCase {

  override protected lazy val configurationProducer: AbstractTestConfigurationProducer[?] =
    ScalaTestConfigurationProducer()

  override protected def runFileStructureViewTest(testClassName: String, status: Int, tests: String*): Unit = {
    val testsModified: Seq[String] = status match {
      case NormalStatusId  => tests
      case IgnoredStatusId => tests.map(_ + TestNodeProvider.IgnoredSuffix)
      case PendingStatusId => tests.map(_ + TestNodeProvider.PendingSuffix)
      case unknownStatus   => fail(s"unknown status code: $unknownStatus").asInstanceOf[Nothing]
    }
    super.runFileStructureViewTest(testClassName, status, testsModified*)
  }
}
