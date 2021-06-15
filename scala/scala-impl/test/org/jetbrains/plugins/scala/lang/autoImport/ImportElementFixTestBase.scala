package org.jetbrains.plugins.scala.lang.autoImport

import com.intellij.codeInsight.JavaProjectCodeInsightSettings
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.autoImport.quickFix.{ElementToImport, ScalaImportElementFix}
import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestAdapter
import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestAdapter.normalize
import org.junit.Assert.{assertEquals, fail}

import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

abstract class ImportElementFixTestBase[Psi <: PsiElement : ClassTag]
  extends ScalaLightCodeInsightFixtureTestAdapter with ScalaFiles {

  def createFix(element: Psi): Option[ScalaImportElementFix[? <: ElementToImport]]

  def checkElementsToImport(fileText: String, expectedQNames: String*): Unit = {
    val fix = configureAndCreateFix(fileText)
    assertEquals("Wrong elements to import found: ", expectedQNames, fix.elements.map(_.qualifiedName))
  }

  def checkNoImportFix(fileText: String): Unit = {
    val fix = configureAndCreateFix(fileText)
    assertEquals(s"Some elements to import found ${fix.elements.map(_.qualifiedName)}", Seq.empty, fix.elements)
  }

  def doTest(fileText: String, expectedText: String, selected: String): Unit = {
    val fix = configureAndCreateFix(fileText)
    val action = fix.createAddImportAction(getEditor)

    fix.elements.find(_.qualifiedName == selected) match {
      case None       => fail(s"No elements found with qualified name $selected")
      case Some(elem) => action.addImportTestOnly(elem)
    }
    assertEquals("Result doesn't match expected text", normalize(expectedText), normalize(getFile.getText))
  }

  private def configureAndCreateFix(fileText: String): ScalaImportElementFix[? <: ElementToImport] = {
    val file = configureFromFileText(fileText, fileType)
    val clazz = implicitly[ClassTag[Psi]].runtimeClass.asInstanceOf[Class[Psi]]
    val element = PsiTreeUtil.findElementOfClassAtOffset(file, getEditorOffset, clazz, false)
    createFix(element).getOrElse(throw NoFixException(element))
  }

  protected def withExcluded(qNames: String*)(body: => Unit): Unit =
    ImportElementFixTestBase.withExcluded(getProject, qNames)(body)

  private case class NoFixException(element: PsiElement)
    extends AssertionError(s"Import fix not found for ${element.getText}")
}

object ImportElementFixTestBase {

  def withExcluded(project: Project, qNames: Seq[String])(body: => Unit): Unit = {
    val settings = JavaProjectCodeInsightSettings.getSettings(project)
    val originalNames = settings.excludedNames
    settings.excludedNames = qNames.asJava

    try body
    finally settings.excludedNames = originalNames
  }

}
