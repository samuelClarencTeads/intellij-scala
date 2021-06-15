package org.jetbrains.plugins.scala
package refactoring.introduceField

import java.io.File

import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.{CharsetToolkit, LocalFileSystem}
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.plugins.scala.base.ScalaLightPlatformCodeInsightTestCaseAdapter
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, executeWriteActionCommand}
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.Int
import org.jetbrains.plugins.scala.lang.refactoring.introduceField.{IntroduceFieldContext, IntroduceFieldSettings, ScalaIntroduceFieldFromExpressionHandler}
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaRefactoringUtil.getExpressionWithTypes
import org.junit.Assert.*

import scala.annotation.nowarn

/**
 * Nikolay.Tropin
 * 7/17/13
 */
@nowarn("msg=ScalaLightPlatformCodeInsightTestCaseAdapter")
abstract class IntroduceFieldTestBase() extends ScalaLightPlatformCodeInsightTestCaseAdapter {
  private val startMarker = "/*start*/"
  private val endMarker = "/*end*/"
  private val replaceAllMarker = "/*replaceAll*/"
  private val initInDeclarationMarker = "/*initInDeclaration*/"
  private val initLocallyMarker = "/*initLocally*/"
  private val selectedClassNumberMarker = "/*selectedClassNumber = "

  def folderPath: String = baseRootPath + "introduceField/"

  implicit def projectContext: Project = getProjectAdapter

  protected def doTest(scType: ScType = Int): Unit = {
    val filePath = folderPath + getTestName(false) + ".scala"
    val file = LocalFileSystem.getInstance.findFileByPath(filePath.replace(File.separatorChar, '/'))
    assert(file != null, "file " + filePath + " not found")
    var fileText = StringUtil.convertLineSeparators(FileUtil.loadFile(new File(file.getCanonicalPath), CharsetToolkit.UTF8))

    val startOffset = fileText.indexOf(startMarker)
    assert(startOffset != -1, "Not specified start marker in test case. Use /*start*/ in scala file for this.")
    fileText = fileText.replace(startMarker, "")

    val endOffset = fileText.indexOf(endMarker)
    assert(endOffset != -1, "Not specified end marker in test case. Use /*end*/ in scala file for this.")
    fileText = fileText.replace(endMarker, "")

    configureFromFileTextAdapter(getTestName(false) + ".scala", fileText)
    val scalaFile = getFileAdapter.asInstanceOf[ScalaFile]
    val editor = getEditorAdapter
    editor.getSelectionModel.setSelection(startOffset, endOffset)

    var res: String = null

    val lastPsi = scalaFile.findElementAt(scalaFile.getText.length - 1)
    val replaceAll = fileText.contains(replaceAllMarker)
    val initInDecl = if (fileText.contains(initInDeclarationMarker)) Some(true)
    else if (fileText.contains(initLocallyMarker)) Some(false)
    else None
    val selectedClassNumber = fileText.indexOf(selectedClassNumberMarker) match {
      case -1 => 0
      case idx: Int => fileText.charAt(idx + selectedClassNumberMarker.length).toString.toInt
    }
    
    //start to inline
    try {
      val handler = new ScalaIntroduceFieldFromExpressionHandler
      val Some((expr, types)) = getExpressionWithTypes(scalaFile, startOffset, endOffset)(getProjectAdapter, editor)
      val aClass = expr.parents.toList.filter(_.isInstanceOf[ScTemplateDefinition])(selectedClassNumber).asInstanceOf[ScTemplateDefinition]
      val ifc = new IntroduceFieldContext[ScExpression](getProjectAdapter, editor, scalaFile, expr, types, aClass)
      val settings = new IntroduceFieldSettings[ScExpression](ifc)
      settings.replaceAll = replaceAll
      initInDecl.foreach(settings.initInDeclaration = _)
      settings.defineVar = true
      settings.name = "i"
      settings.scType = scType

      executeWriteActionCommand("Test", UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION) {
        handler.runRefactoring(ifc, settings)
        UsefulTestCase.doPostponedFormatting(getProjectAdapter)
      }

      res = scalaFile.getText.substring(0, lastPsi.getTextOffset).trim
    }
    catch {
      case e: Exception => assert(assertion = false, message = e.getMessage + "\n" + e.getStackTrace.map(_.toString).mkString("  \n"))
    }

    val text = lastPsi.getText
    val output = lastPsi.getNode.getElementType match {
      case ScalaTokenTypes.tLINE_COMMENT => text.substring(2).trim
      case ScalaTokenTypes.tBLOCK_COMMENT | ScalaTokenTypes.tDOC_COMMENT =>
        text.substring(2, text.length - 2).trim
      case _ =>
        assertTrue("Test result must be in last comment statement.", false)
        ""
    }
    
    assertEquals(output, res)
  }
}
