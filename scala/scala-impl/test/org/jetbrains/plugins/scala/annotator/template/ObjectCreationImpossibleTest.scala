package org.jetbrains.plugins.scala.annotator.template

import org.jetbrains.plugins.scala.annotator.element.ScTemplateDefinitionAnnotator
import org.jetbrains.plugins.scala.annotator.element.ScTemplateDefinitionAnnotator.*
import org.jetbrains.plugins.scala.annotator.{AnnotatorTestBase, Error, ScalaAnnotationHolder}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition

/**
 * Pavel Fatin
 */

class ObjectCreationImpossibleTest extends AnnotatorTestBase[ScTemplateDefinition] {
  def testFineNew(): Unit = {
    assertNothing(messages("class C; new C"))
    assertNothing(messages("class C; new C {}"))
    assertNothing(messages("class C; trait T; new C with T"))
    assertNothing(messages("class C; trait T; new C with T {}"))
  }

  def testFineObject(): Unit = {
    assertNothing(messages("class C; object O extends C"))
    assertNothing(messages("class C; object O extends C {}"))
    assertNothing(messages("class C; trait T; object O extends C with T"))
    assertNothing(messages("class C; trait T; object O extends C with T {}"))
  }

  def testTypeSkipDeclarations(): Unit = {
    assertNothing(messages("class C { def f }"))
  }

  def testSkipAbstractInstantiations(): Unit = {
    assertNothing(messages("trait T; new T"))
  }

  def testSkipConcrete(): Unit = {
    assertNothing(messages("class C { def f }; new C"))
    assertNothing(messages("class C { def f }; new C {}"))
    assertNothing(messages("class C { def f }; new Object with C"))
    assertNothing(messages("class C { def f }; new Object with C {}"))
  }

  def testSkipInvalidDirect(): Unit = {
    assertNothing(messages("new { def f }"))
    assertNothing(messages("new Object { def f }"))
    assertNothing(messages("object O { def f }"))
  }

  def testUndefinedMember(): Unit = {
    val Message = objectCreationImpossibleMessage(("f: Unit", "Holder.T"))

    assertMatches(messages("trait T { def f }; new T {}")) {
      case Error("T", Message) :: Nil =>
    }
  }

  def testUndefinedMemberObject(): Unit = {
    val Message = objectCreationImpossibleMessage(("f: Unit", "Holder.T"))

    assertMatches(messages("trait T { def f }; object O extends T {}")) {
      case Error("O", Message) :: Nil =>
    }
  }

  def testUndefinedAndWith(): Unit = {
    val Message = objectCreationImpossibleMessage(("f: Unit", "Holder.T"))

    assertMatches(messages("trait T { def f }; new Object with T {}")) {
      case Error("Object", Message) :: Nil =>
    }
  }

  def testNeedsToBeAbstractPlaceDiffer(): Unit = {
    val Message = objectCreationImpossibleMessage(
      ("b: Unit", "Holder.B"), ("a: Unit", "Holder.A"))
    val ReversedMessage = objectCreationImpossibleMessage(
      ("a: Unit", "Holder.A"), ("b: Unit", "Holder.B"))

    assertMatches(messages("trait A { def a }; trait B { def b }; new A with B {}")) {
      case Error("A", Message) :: Nil =>
      case Error("A", ReversedMessage) :: Nil =>
    }
  }

  def testSkipTypeDeclarationSCL2887(): Unit = {
    assertNothing(messages("trait A { type a }; new A {}"))
  }

  override protected def annotate(element: ScTemplateDefinition)
                                 (implicit holder: ScalaAnnotationHolder): Unit =
    ScTemplateDefinitionAnnotator.annotateObjectCreationImpossible(element)
}