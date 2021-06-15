package org.jetbrains.plugins.scala.lang.resolve
import org.jetbrains.plugins.scala.{LatestScalaVersions, ScalaVersion}
import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestAdapter

class ParenlessMethodCallOverloadingResolutionTest
    extends ScalaLightCodeInsightFixtureTestAdapter
    with SimpleResolveTestBase {
  import SimpleResolveTestBase.*

  override protected def supportedIn(version: ScalaVersion): Boolean =
    version  >= LatestScalaVersions.Scala_2_12

  def testSCL16802(): Unit = doResolveTest(
    s"""
       |trait Foo {
       |  def foo(i: Int): String
       |}
       |
       |def ge${REFTGT}tFoo(): Foo = ???
       |def getFoo(s: String): Foo = ???
       |
       |def takesFoo(foo: Foo): Unit = ()
       |takesFoo(getF${REFSRC}oo)
       |
       |""".stripMargin)

}
