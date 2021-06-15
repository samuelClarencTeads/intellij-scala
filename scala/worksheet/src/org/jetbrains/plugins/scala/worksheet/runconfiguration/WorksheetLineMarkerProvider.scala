package org.jetbrains.plugins.scala.worksheet.runconfiguration

import java.{util as ju}

import com.intellij.codeInsight.daemon.{LineMarkerInfo, LineMarkerProvider}
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.{Document, EditorFactory}
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{PsiComment, PsiDocumentManager, PsiElement, PsiWhiteSpace}
import com.intellij.util.FunctionUtil
import org.jetbrains.plugins.scala.extensions.{IteratorExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.worksheet.{WorksheetBundle, WorksheetFile}

import scala.jdk.CollectionConverters.*

class WorksheetLineMarkerProvider extends LineMarkerProvider {

  override def getLineMarkerInfo(psiElement: PsiElement): LineMarkerInfo[?] = null

  override def collectSlowLineMarkers(elements: ju.List[? <: PsiElement],
                                      result: ju.Collection[? >: LineMarkerInfo[?]]): Unit =
    // assuming that all elements are from the same file
    for {
      firstElement <- elements.iterator.asScala.headOption
      scalaFile    <- worksheetFile(firstElement)
      marker       <- lineMarkerInfo(elements, scalaFile)
    } result.add(marker)

  private def worksheetFile(element: PsiElement): Option[WorksheetFile] =
    element.getContainingFile match {
      case file: WorksheetFile if file.isRepl =>
        Some(file)
      case _ =>
        None
    }

  private def lineMarkerInfo(elements: ju.List[? <: PsiElement], scalaFile: ScalaFile): Option[LineMarkerInfo[PsiElement]] = {
    val project = scalaFile.getProject
    for {
      document          <- Option(PsiDocumentManager.getInstance(project).getCachedDocument(scalaFile))
      editor            <- EditorFactory.getInstance().getEditors(document, project).headOption
      lastProcessedLine <- WorksheetCache.getInstance(project).getLastProcessedIncremental(editor)
      element           <- findMarkerAnchorElement(elements, document, lastProcessedLine)
    } yield createArrowMarker(element)
  }

  private def findMarkerAnchorElement(elements: ju.List[? <: PsiElement],
                                      document: Document,
                                      lastProcessedLine: Int): Option[PsiElement] =
    elements.iterator.asScala
      .filter(!isEmpty(_))
      .find(el => document.getLineNumber(el.startOffset) == lastProcessedLine)

  private def isEmpty(element: PsiElement): Boolean = element match {
    case _: PsiComment | _: PsiWhiteSpace    => true
    case empty if empty.getTextRange.isEmpty => true
    case _                                   => false
  }

  private def createArrowMarker(psiElement: PsiElement): LineMarkerInfo[PsiElement] = {
    val leaf = PsiTreeUtil.firstChild(psiElement)
    new LineMarkerInfo[PsiElement](
      leaf,
      leaf.getTextRange,
      AllIcons.Actions.Forward,
      FunctionUtil.nullConstant[PsiElement, String],
      null,
      GutterIconRenderer.Alignment.RIGHT,
      () => WorksheetBundle.message("accessibility.worksheet.arrow")
    )
  }
}