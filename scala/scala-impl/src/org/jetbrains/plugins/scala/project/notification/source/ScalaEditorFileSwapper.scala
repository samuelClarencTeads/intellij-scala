package org.jetbrains.plugins.scala
package project.notification.source

import com.intellij.openapi.fileEditor.impl.{EditorFileSwapper, EditorWithProviderComposite}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager

object ScalaEditorFileSwapper {
  def findSourceFile(project: Project, eachFile: VirtualFile): VirtualFile =
    PsiManager.getInstance(project).findFile(eachFile) match {
      case file: ScalaFile if file.isCompiled =>
        val fqn: String = getFQN(file)
        if (fqn == null) return null

        var clazz: PsiClass = null
        for {
          cl <- ScalaPsiManager.instance(project).getCachedClasses(file.resolveScope, fqn)
          if clazz == null && cl.getContainingFile == file
        } clazz = cl

        if (!clazz.isInstanceOf[ScTypeDefinition]) return null
        val sourceClass: PsiClass = clazz.asInstanceOf[ScTypeDefinition].getSourceMirrorClass
        if (sourceClass == null || (sourceClass eq clazz)) return null
        val result: VirtualFile = sourceClass.getContainingFile.getVirtualFile
        assert(result != null)
        result
      case _ => null
    }

  def getFQN(scalaFile: ScalaFile): String =
    scalaFile.typeDefinitions.headOption.map(_.qualifiedName).orNull
}

class ScalaEditorFileSwapper extends EditorFileSwapper {
  override def getFileToSwapTo(project: Project,
                               editorWithProviderComposite: EditorWithProviderComposite): Pair[VirtualFile, Integer] = {
    Pair.create(ScalaEditorFileSwapper.findSourceFile(project, editorWithProviderComposite.getFile), null)
  }
}