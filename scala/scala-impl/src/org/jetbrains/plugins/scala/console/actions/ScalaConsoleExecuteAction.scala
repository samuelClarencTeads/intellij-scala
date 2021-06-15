package org.jetbrains.plugins.scala.console.actions

import java.io.{IOException, OutputStream}

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, CommonDataKeys}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import org.jetbrains.plugins.scala.console.ScalaConsoleInfo
import org.jetbrains.plugins.scala.console.actions.ScalaConsoleExecuteAction.*
import org.jetbrains.plugins.scala.{ScalaBundle, inWriteAction}

class ScalaConsoleExecuteAction extends AnAction(
  ScalaBundle.message("execute.scala.repl.statement.menu.action.text"),
  ScalaBundle.message("execute.scala.repl.statement.menu.action.description"),
  /* icon = */ null
) with DumbAware {

  override def update(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    if (editor == null || !editor.isInstanceOf[EditorEx]) {
      e.getPresentation.setEnabled(false)
      return
    }
    val console = ScalaConsoleInfo.getConsole(editor)
    if (console == null) {
      e.getPresentation.setEnabled(false)
      return
    }
    val terminated = ScalaConsoleInfo.getProcessHandler(editor).isProcessTerminated
    val isEnabled: Boolean = !editor.asInstanceOf[EditorEx].isRendererMode && !terminated
    e.getPresentation.setEnabled(isEnabled)
  }

  override def actionPerformed(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    if (editor == null) return

    val console           = ScalaConsoleInfo.getConsole(editor)
    val processHandler    = ScalaConsoleInfo.getProcessHandler(editor)
    val historyController = ScalaConsoleInfo.getController(editor)

    if (editor == null || console == null || processHandler == null || historyController == null) {
      LOG.info(new Throwable(s"Enter action in console failed: $editor, $console"))
      return
    }

    val document = console.getEditorDocument
    val text = document.getText

    // Process input and add to history
    inWriteAction {
      val range: TextRange = new TextRange(0, document.getTextLength)
      editor.getSelectionModel.setSelection(range.getStartOffset, range.getEndOffset)
      console.addToHistory(range, console.getConsoleEditor, true)
      historyController.addToHistory(text)

      editor.getCaretModel.moveToOffset(0)
      editor.getDocument.setText("")
    }

    val lines = text.split('\n')
    lines.foreach { line =>
      val lineWithFeed = line + "\n"
      try {
        val outputStream: OutputStream = processHandler.getProcessInput
        val bytes: Array[Byte] = lineWithFeed.getBytes
        outputStream.write(bytes)
        outputStream.flush()
      } catch {
        case ex: IOException =>
          val MaxLogStringLength = 1000
          LOG.warn(s"Unexpected exception occurred during writing to process input:\n`${line.substring(MaxLogStringLength)}`", ex)
      }
      console.textSent(lineWithFeed)
    }
  }
}

object ScalaConsoleExecuteAction {
  private val LOG = Logger.getInstance(this.getClass)
}
