package org.jetbrains.plugins.scala
package decompileToJava

import java.{util as ju}

import com.intellij.codeInsight.AttachSourcesProvider
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.util.ActionCallback
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.scala.lang.psi.api.ScFile

class DecompileScalaToJavaActionProvider extends AttachSourcesProvider {

  import AttachSourcesProvider.AttachSourcesAction

  override def getActions(list: ju.List[LibraryOrderEntry],
                          classFile: PsiFile): ju.Collection[AttachSourcesAction] =
    classFile match {
      case file: ScFile if file.isCompiled =>
        val action = new AttachSourcesAction {
          override def getName: String = "Decompile to Java"

          override def getBusyText: String = "Scala Classfile"

          override def perform(list: ju.List[LibraryOrderEntry]): ActionCallback = {
            ScalaBytecodeDecompileTask.showDecompiledJavaCode(file)
            ActionCallback.DONE
          }
        }
        ju.Collections.singletonList(action)
      case _ => ju.Collections.emptyList()
    }
}
