package org.jetbrains.plugins.scala
package decompileToJava

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, CommonDataKeys, DataKey}
import org.jetbrains.plugins.scala.lang.psi.api.ScFile

class DecompileScalaToJavaAction extends AnAction(JavaDecompilerBundle.message("decompile.scala.to.java")) {
  override def actionPerformed(event: AnActionEvent): Unit =
    getClassfile(event).foreach(ScalaBytecodeDecompileTask.showDecompiledJavaCode)

  override def update(event: AnActionEvent): Unit = {
    val project = event.getProject
    val classFile = getClassfile(event)
    event.getPresentation.setEnabledAndVisible(project != null && classFile.isDefined)
  }

  private[this] def getClassfile(event: AnActionEvent): Option[ScFile] = {
    import CommonDataKeys.*
    findClassFile(event, NAVIGATABLE)
      .orElse(findClassFile(event, PSI_FILE))
  }

  private[this] def findClassFile(event: AnActionEvent, key: DataKey[?]): Option[ScFile] =
    event.getData(key) match {
      case file: ScFile if file.isCompiled => Option(file)
      case _ => None
    }
}