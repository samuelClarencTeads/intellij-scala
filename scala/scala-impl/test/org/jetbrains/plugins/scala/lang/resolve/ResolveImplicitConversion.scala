package org.jetbrains.plugins.scala.lang.resolve

import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.junit.Assert.*

/**
  * Created by kate on 6/15/16.
  */
class ResolveImplicitConversion extends ScalaResolveTestCase {
  override def folderPath: String = super.folderPath + "resolve/implicitConversion"

  def doTest(): Unit = {
    findReferenceAtCaret() match {
      case ref: ScReference =>
        val variants = ref.multiResolveScala(false)
        assertTrue(s"Single resolve expected, was: ${variants.length}", variants.length == 1)
    }
  }

  def testScl4968(): Unit = doTest()

  def testSCL8757(): Unit = doTest()

  def testSCL8660(): Unit = doTest()

  def testScl7974(): Unit = doTest()

  def testSCL10670(): Unit = doTest()

  def testSCL10549(): Unit = doTest()

  def testSCL9224(): Unit = doTest()

  def testSCL12251(): Unit = doTest()

  def testSCL13686(): Unit = doTest()
}
