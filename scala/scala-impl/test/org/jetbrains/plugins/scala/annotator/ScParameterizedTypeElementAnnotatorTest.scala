package org.jetbrains.plugins.scala.annotator

import org.jetbrains.plugins.scala.annotator.element.ScParameterizedTypeElementAnnotator
import org.jetbrains.plugins.scala.base.SimpleTestCase
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScParameterizedTypeElement

class ScParameterizedTypeElementAnnotatorTest extends SimpleTestCase {
  def testTooFewTypeParameter(): Unit = {
    assertMessagesInAllContexts("Test[Int]")(
      Error("t]", "Unspecified type parameters: Y")
    )
    assertMessagesInAllContexts("Test[]")(
      Error("[]", "Unspecified type parameters: X, Y")
    )
  }

  def testTooManyTypeParameter(): Unit = {
    assertMessagesInAllContexts("Test[Int, Int, Int]")(
      Error(", I", "Too many type arguments for Test[X, Y]")
    )
    assertMessagesInAllContexts("Test[Int, Int, Boolean, Int]")(
      Error(", B", "Too many type arguments for Test[X, Y]")
    )

    assertMessagesInAllContexts("A[Int]")(
      Error("[Int]", "A does not take type arguments")
    )
  }

  def testBoundViolation(): Unit = {
    assertMessagesInAllContexts("Test2[A, A]")(
      Error("A", "Type A does not conform to upper bound B of type parameter X"),
    )

    assertMessagesInAllContexts("Test2[C, C]")(
      Error("C", "Type C does not conform to lower bound B of type parameter Y"),
    )

    assertMessagesInAllContexts("Test2[A, C]")(
      Error("A", "Type A does not conform to upper bound B of type parameter X"),
      Error("C", "Type C does not conform to lower bound B of type parameter Y"),
    )
  }

  def testHigherKindedTypes(): Unit = {
    assertMessagesInAllContexts("TestHk[Test]")()

    assertMessagesInAllContexts("TestHk[A]")(
      Error("A", "Expected type constructor CC[X >: B <: B, _]")
    )

    assertMessagesInAllContexts("TestHk[HkArg]")(
      Error("HkArg", "Type constructor HkArg does not conform to CC[X >: B <: B, _]")
    )
  }

  def testTypeConstructorParameter(): Unit = {
    assertNothing(messages(
      """
        |trait Functor[F[_]]
        |
        |trait Applicative[G[_]] extends Functor[G]
        |
        |""".stripMargin
    ))
  }

  def testInferredUpperbound(): Unit = {
    assertNothing(messages(
      """
        |trait A
        |trait B extends A
        |
        |class Test[Up, X <: Up]
        |
        |new Test[A, B]
        |
        |""".stripMargin
    ))
  }

  def testSelfInUpperBound(): Unit = {
    assertNothing(messages(
      """
        |trait Base[A]
        |class Impl extends Base[Impl]
        |
        |class Test[X <: Base[X]]
        |
        |
        |new Test[Impl]
        |
        |""".stripMargin
    ))
  }

  def testOuterTypeParameter(): Unit = {
    assertMessages(messages(
      """
        |class Context[C <: Ctx, Ctx] {
        |
        |  class Test[X >: Ctx <: Ctx]
        |
        |  new Test[C]
        |}
        |""".stripMargin
    ))(
      Error("C", "Type C does not conform to lower bound Ctx of type parameter X")
    )
  }

  def testHkBound(): Unit = {

    assertNothing(messages(
      """
        |trait M[F]
        |trait S[X[A] <: M[X]]
        |
        |class Test[Y[_] <: M[Y]] extends S[Y]
        |""".stripMargin
    ))
  }

  def testTypeBoundContainingTypeParamInTypeConstructor(): Unit = {
    assertNothing(messages(
      """
        |trait Trait[X]
        |trait HkType[A[X <: Trait[X]]] {
        |    def test[B[Y <: Trait[Y]]]: HkType[B]
        |}
        |""".stripMargin
    ))
  }

  def testProjectionTypes(): Unit = {
    assertNothing(messages(
      """
        |sealed trait GenericList[U] {
        |  type Transformed[N <: U]
        |}
        |
        |trait GenericCons[U] {
        |  type Transformed[N <: U] = GenericList[U]#Transformed[N]
        |}
        |""".stripMargin
    ))
  }

  def testAnyAsTypeConstructor(): Unit = {
    assertNothing(messages(
      """
        |trait Hk[TC[_]]
        |
        |new Hk[Any]
        |""".stripMargin
    ))
  }

  def testNothingAsTypeConstructor(): Unit = {
    assertNothing(messages(
      """
        |trait Hk[TC[_]]
        |
        |new Hk[Nothing]
        |""".stripMargin
    ))
  }

  def testWildcard(): Unit = {
    assertNothing(messages(
      """
        |trait Growable
        |trait LazyCombiner[Buff <: Growable]
        |
        |object Test {
        |  null.isInstanceOf[LazyCombiner[_]]
        |}
        |
        |""".stripMargin
    ))
  }

  def testExistentialType(): Unit = {
    assertNothing(messages(
      """
        |trait Growable
        |trait LazyCombiner[Buff <: Growable]
        |
        |object Test {
        |  null.isInstanceOf[LazyCombiner[X] forSome { type X }]
        |}
        |
        |""".stripMargin
    ))
  }

  def testUnresolved(): Unit = {
    assertNothing(messages(
      """
        |trait A
        |trait B extends A
        |
        |trait Test[X <: A]
        |new Test[hahaha]
        |
        |""".stripMargin
    ))
  }

  def testTypePattern(): Unit = {
    assertNothing(messages(
      """
        |trait Bar
        |class Foo[t <: Bar] {}
        |
        |new Foo[Bar] match {
        |  case _ : Foo[x] => null: Foo[x]
        |}
        |""".stripMargin
    ))
  }

  def testUpperBoundIsTypeConstructorSetToAny(): Unit = {
    assertNothing(messages(
      """
        |trait Test[X <: Upper[X], Upper[_]]
        |
        |def test[T]: Test[T, Any]
        |""".stripMargin
    ))
  }

  def testLowerBoundIsTypeConstructorSetToNothing(): Unit = {
    assertNothing(messages(
      """
        |trait A
        |trait Test[X >: Lower[X], Lower[_]]
        |
        |def test[T >: A]: Test[T, Nothing]
        |""".stripMargin
    ))
  }

  def testGenericTraversalTemplate(): Unit = {
    assertNothing(messages(
      """
        |// Streamlined version of tcpoly_infer_implicit_tuple_wrapper.scala
        |trait Iterable[A]
        |trait GenericTraversableTemplate[DD[X] <: Iterable[X]]
        |
        |class IterableOps[CC[B] <: Iterable[B] with GenericTraversableTemplate[CC]]
        |
        |""".stripMargin
    ))
  }

  def testParameterizedProjectionType(): Unit = {
    assertNothing(messages(
      """
        |trait ParIterableLike[+TT] {
        |  class Copy[UU >: TT]
        |}
        |
        |trait ParSeqLike[+T] extends ParIterableLike[T] {
        |  def test[U >: T]() = {
        |    new Copy[U]
        |  }
        |}
        |""".stripMargin
    ))
  }

  def testBoundTypeConstructorInUpperBoundOfTypeConstructor(): Unit = {
    assertNothing(messages(
      """
        |trait SetLike[K, +This <: SetLike[K, This]]
        |trait SetFactory[CC[A] <: SetLike[A, CC[A] /* <- interesting */]]
        |
        |// Interesting check is
        |//   CC[A] <: SetLike[A, CC[A]]
        |// which should check upper bound of CC, which should result in the following check:
        |//   [A] SetLike[A, CC[A]] <: [A] SetLike[A, CC[A]]
        |""".stripMargin
    ))
  }

  def assertMessagesInAllContexts(typeText: String)(expected: Message*): Unit = {
    val Header =
      """
        |trait A
        |trait B extends A
        |trait C extends B
        |
        |class Test[X, Y]
        |class Test2[X <: B, Y >: B]
        |class TestHk[CC[X >: B <: B, _]]
        |
        |class HkArg[X >: A, Y <: C]
        |""".stripMargin

    val contexts = Seq(
      s"type W = $typeText",
      s"def w(arg: $typeText): Unit = ()",
      s"def w[Q](arg: $typeText): Unit = ()",
      s"def w(): $typeText = w()",
      s"new $typeText",
      s"class Blub extends $typeText",
      s"class W[Q <: $typeText]",
    )

    for (context <- contexts) {
      assertMessages(messages(Header + context))(expected*)
    }
  }

  def messages(code: String): List[Message] = {
    val file: ScalaFile = code.parse

    implicit val mock: AnnotatorHolderMock = new AnnotatorHolderMock(file)

    file.depthFirst().filterByType[ScParameterizedTypeElement].foreach { pte =>
      ScParameterizedTypeElementAnnotator.annotate(pte, typeAware = true)
    }

    mock.annotations
  }
}
