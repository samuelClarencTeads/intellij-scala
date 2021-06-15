package org.jetbrains.plugins.scala
package projectView

import com.intellij.ide.IconProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import javax.swing.Icon
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

final class ScalaIconProvider extends IconProvider {

  override def getIcon(element: PsiElement, flags: Int): Icon = element match {
    case file: ScalaFile =>
      ProgressManager.checkCanceled()
      Node(file)(null, null).getIcon(flags)
    case _ => null
  }
}