package org.jetbrains.plugins.scala.worksheet.actions.topmenu

import java.awt.datatransfer.StringSelection

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.editor.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.{PsiDocumentManager, PsiFile}
import javax.swing.Icon
import org.jetbrains.plugins.scala.extensions.StringExt
import org.jetbrains.plugins.scala.worksheet.WorksheetBundle
import org.jetbrains.plugins.scala.worksheet.runconfiguration.WorksheetCache
import org.jetbrains.plugins.scala.worksheet.ui.WorksheetFoldGroup

class CopyWorksheetAction extends AnAction(
  WorksheetBundle.message("copy.scala.worksheet.action.text"),
  WorksheetBundle.message("copy.scala.worksheet.action.description"),
  AllIcons.Actions.Copy
) with TopComponentAction {

  override def genericText: String = WorksheetBundle.message("worksheet.copy.button")

  override def actionIcon: Icon = AllIcons.Actions.Copy

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.getInstance(project).getSelectedTextEditor
    if (editor == null) return

    val resultText = CopyWorksheetAction.prepareCopiableText(editor, project).getOrElse(return)

    val contents: StringSelection = new StringSelection(resultText)
    CopyPasteManager.getInstance.setContents(contents)
  }
}

object CopyWorksheetAction {

  private val COPY_BORDER = 80
  private val FILL_SYMBOL = " "

  def prepareCopiableText(editor: Editor, project: Project): Option[String] = {
    val psiFile: PsiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument)
    val viewer = WorksheetCache.getInstance(project).getViewer(editor)
    if (psiFile == null) return None

    Some(createMerged2(editor, viewer, psiFile).withNormalizedSeparator)
  }

  private def createMerged2(editor: Editor, viewer: Editor, psiFile: PsiFile): String = {
    val leftDocument = editor.getDocument
    if (viewer == null) return leftDocument.getText
    val rightDocument = viewer.getDocument
    if (rightDocument.getTextLength == 0) return leftDocument.getText

    val result = new StringBuilder
    val fullShift = StringUtil.repeat(" ", COPY_BORDER)
    val lineSeparator = "\n"

    val mappings = WorksheetFoldGroup.computeMappings(viewer, editor, psiFile.getVirtualFile)

    def getFromDoc(lineNumber: Int, document: Document): CharSequence =
      if (lineNumber >= document.getLineCount) "" else {
        val start = document.getLineStartOffset(lineNumber)
        val end   = document.getLineEndOffset(lineNumber)
        document.getImmutableCharSequence.subSequence(start, end)
      }

    def getLinesFrom(f: Int, t: Int, document: Document) =
      for (i <- f until t) yield getFromDoc(i, document)

    def getLines(doc: Document) = getLinesFrom(0, doc.getLineCount, doc)

    def append2Result(leftLines: Seq[CharSequence], rightLines: Seq[CharSequence]): Unit = {
      leftLines.zipAll(rightLines, "", fullShift).foreach {
        case (textLeft, textRight) =>
          result.append(StringUtil.trimTrailing(textLeft))

          if (textRight.length() > 0) {
            for (_ <- 1 to (COPY_BORDER - textLeft.length)) {
              result.append(FILL_SYMBOL)
            }
            result.append(" //")
            result.append(textRight)
          }

          result.append(lineSeparator)
      }
    }

    if (mappings.length < 2) {
      append2Result(getLines(leftDocument), getLines(rightDocument))
    } else {
      (mappings.tail :+ (leftDocument.getLineCount, rightDocument.getLineCount)).foldLeft(mappings.head) {
        case ((pl, pr), (l, r)) =>
          append2Result(getLinesFrom(pl, l, leftDocument), getLinesFrom(pr, r, rightDocument))
          (l, r)
      }
    }

    result.toString()
  }
}
