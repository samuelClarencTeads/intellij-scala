package org.jetbrains.plugins.scala.externalLibraries.monocle

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.DependencyManagerBase.*
import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestAdapter.normalize
import org.jetbrains.plugins.scala.base.ScalaLightPlatformCodeInsightTestCaseAdapter
import org.jetbrains.plugins.scala.base.libraryLoaders.*
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.lang.psi.types.result.*
import org.jetbrains.plugins.scala.util.TestUtils
import org.junit.Assert.*

import scala.annotation.nowarn

@nowarn("msg=ScalaLightPlatformCodeInsightTestCaseAdapter")
class MonocleLensesTest extends ScalaLightPlatformCodeInsightTestCaseAdapter {

  protected val (monocleOrg, monocleVer) = ("com.github.julien-truffaut", "1.5.0")

  override protected def additionalLibraries: Seq[LibraryLoader] =
    IvyManagedLoader(
      monocleOrg %% "monocle-core"    % monocleVer,
      monocleOrg %% "monocle-macro"   % monocleVer,
      monocleOrg %% "monocle-generic" % monocleVer
    ) :: Nil

  protected def folderPath: String = TestUtils.getTestDataPath

  def doTest(text: String, methodName: String, expectedType: String): Unit = {
    val normalized = normalize(text)
    val caretPos = normalized.indexOf("<caret>")
    configureFromFileTextAdapter("dummy.scala", normalized.replace("<caret>", ""))
    val exp = PsiTreeUtil.findElementOfClassAtOffset(getFileAdapter, caretPos, classOf[ScalaPsiElement], false).asInstanceOf[ScObject]
    exp.allMethods.find(_.name == methodName) match {
      case Some(x) => x.method.asInstanceOf[ScFunctionDefinition].returnType match {
        case Right(t) => assertEquals(s"${t.toString} != $expectedType", expectedType, t.toString)
        case Failure(cause) => fail(cause)
      }
      case None => fail("method not found")
    }
  }

  def testSimple(): Unit = {
    val fileText: String =
      """
        |import monocle.macros.Lenses
        |
        |object Main {
        |  @Lenses
        |  case class Person(name: String, age: Int, address: Address)
        |  @Lenses
        |  case class Address(streetNumber: Int, streetName: String)
        |
        |  object <caret>Person {
        |    import Main.Address._
        |    val john = Person("John", 23, Address(10, "High Street"))
        |    age.get(john)
        |  }
        |}
      """.stripMargin

    doTest(fileText, "age", "monocle.Lens[Main.Person, Int]")
  }

  def testTypeArgs(): Unit = {
    val fileText =
      """
        |import monocle.macros.Lenses
        |import monocle.syntax._
        |
        |object Main {
        |
        |  @Lenses
        |  case class Foo[A,B](q: Map[(A,B),Double], default: Double)
        |  object <caret>Foo {}
        |}
      """.stripMargin

    doTest(fileText, "q", "monocle.Lens[Main.Foo[A, B], Map[(A, B), Double]]")
  }

  def testTypeArgsWithBounds(): Unit = {
    val fileText =
      """
        |import monocle.macros.Lenses
        |import monocle.syntax._
        |
        |object Main {
        |  class Foo
        |
        |  @Lenses
        |  case class Bar[A <: Foo](a: A)
        |  object <caret>Bar {}
        |}
      """.stripMargin
    doTest(fileText, "a", "monocle.Lens[Main.Bar[A], A]")
  }


  def testTypeArgsWithBoundsAndVariance(): Unit = { // SCL-15167
    val fileText =
      """
        |import monocle.macros.Lenses
        |import monocle.syntax._
        |
        |object Main {
        |  class Foo
        |
        |  @Lenses
        |  case class Bar[+A <: Foo](a: A)
        |  object <caret>Bar {}
        |}
      """.stripMargin
    doTest(fileText, "a", "monocle.Lens[Main.Bar[A], A]")
  }



  def testRecursion(): Unit = {
    //SCL-9420
    val fileText =
      """
        |object Main {
        |import monocle.macros.Lenses
        |import A.B
        |
        |object <caret>A {
        |  type B = String
        |}
        |
        |@Lenses
        |case class A(s : B) {
        |  def blah = s.getBytes
        |}
        |}
      """.stripMargin

    doTest(fileText, "s", "monocle.Lens[Main.A, Main.A.B]")
  }
}