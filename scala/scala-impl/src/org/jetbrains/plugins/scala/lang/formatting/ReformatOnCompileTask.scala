package org.jetbrains.plugins.scala.lang.formatting

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.compiler.*
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.{PsiFile, PsiManager}
import org.jetbrains.plugins.scala.ScalaFileType
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.ScalafmtDynamicConfigService
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.processors.ScalaFmtPreFormatProcessor
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

final class ReformatOnCompileTask(project: Project) extends CompileTask {
  override def execute(context: CompileContext): Boolean = {
    val scalaSettings: ScalaCodeStyleSettings = CodeStyle.getSettings(project).getCustomSettings(classOf[ScalaCodeStyleSettings])
    if (scalaSettings.REFORMAT_ON_COMPILE) {
      ScalaFmtPreFormatProcessor.inFailSilentMode {
        reformatScopeFiles(context.getCompileScope, scalaSettings)
      }
    }
    true
  }

  private def reformatScopeFiles(compileScope: CompileScope, scalaSettings: ScalaCodeStyleSettings): Unit = for {
    virtualFile <- compileScope.getFiles(ScalaFileType.INSTANCE, true)
    psiFile = inReadAction(PsiManager.getInstance(project).findFile(virtualFile))
    if shouldFormatFile(psiFile, scalaSettings)
    psiFile <- psiFile.asOptionOf[ScalaFile].filterNot(_.isWorksheetFile)
  } {
    invokeAndWait { () =>
      CommandProcessor.getInstance().runUndoTransparentAction { () =>
        CodeStyleManager.getInstance(project).reformat(psiFile)
      }
    }
  }

  private def shouldFormatFile(file: PsiFile, scalaSettings: ScalaCodeStyleSettings): Boolean = {
    if (scalaSettings.USE_SCALAFMT_FORMATTER()) {
      ScalafmtDynamicConfigService.isIncludedInProject(file)
    } else {
      true
    }
  }
}
