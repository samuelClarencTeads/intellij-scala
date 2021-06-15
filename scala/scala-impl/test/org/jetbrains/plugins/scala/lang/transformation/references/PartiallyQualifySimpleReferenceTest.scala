package org.jetbrains.plugins.scala
package lang
package transformation
package references

/**
  * @author Pavel Fatin
  */
class PartiallyQualifySimpleReferenceTest extends TransformerTest(new PartiallyQualifySimpleReference()) {

  def testUnqualified(): Unit = check(
    before = "f",
    after = "O.f"
  )(header =
    """
     object O {
       def f: A = _
     }
     import O.*
    """)

  def testQualified(): Unit = check(
    before = "O.f",
    after = "O.f"
  )(header =
    """
     object O {
       def f: A = _
     }
     import O.*
    """)

  def testIndirect(): Unit = check(
    before = "f",
    after = "O.f"
  )(header =
    """
     class T {
       def f: A = _
     }
     object O extends T
     import O.*
    """)
}
