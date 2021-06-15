package org.jetbrains.plugins.scala

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

/**
 * @author Pavel Fatin
 */
package object projectView {

  private[projectView] type Node = AbstractTreeNode[?]

  private[projectView] object Node {

    import FileNode.*

    def apply(file: ScalaFile)
             (implicit project: Project, settings: ViewSettings): Node & IconableNode = {
      val fileType = file.getFileType
      fileType match {
        case ScalaFileType.INSTANCE =>
          if (file.isScriptFile)
            new ScriptFileNode(file)
          else
            FileKind.unapply(file)
              .flatMap(_.node)
              .getOrElse(new ScalaFileNode(file))
        case fileType               =>
          new DialectFileNode(file, fileType)
      }
    }
  }
}
