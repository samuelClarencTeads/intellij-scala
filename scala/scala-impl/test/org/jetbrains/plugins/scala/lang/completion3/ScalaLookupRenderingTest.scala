package org.jetbrains.plugins.scala
package lang
package completion3

import org.junit.Assert.assertTrue

/**
 * @author Alefas
 * @since 23.03.12
 */
class ScalaLookupRenderingTest extends ScalaCodeInsightTestBase {

  import ScalaCodeInsightTestBase.*

  def testJavaVarargs(): Unit = {
    this.configureJavaFile(
      fileText =
        """package a;
          |
          |public class Java {
          |  public static void foo(int... x) {}
          |}""".stripMargin,
      className = "Java",
      packageName = "a"
    )

    val (_, items) = activeLookupWithItems(
      fileText =
        s"""import a.Java
           |class A {
           |  Java.fo$CARET
           |}""".stripMargin
    )()

    val condition = items.exists {
      hasItemText(_, "foo")(
        itemTextBold = true,
        tailText = "(x: Int*)",
        typeText = "Unit"
      )
    }
    assertTrue(condition)
  }
}
