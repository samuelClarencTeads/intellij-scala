package org.jetbrains.plugins.scala.project.sdkdetect.repository

import java.nio.file.Path
import java.util.stream.{Stream as JStream}

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.{VfsUtilCore, VirtualFile}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.project.template.{PathExt, *}

private[repository] class ProjectLocalDetector(contextDirectory: VirtualFile) extends ScalaSdkDetector {
  override def buildSdkChoice(descriptor: ScalaSdkDescriptor): SdkChoice = ProjectSdkChoice(descriptor)
  override def friendlyName: String = ScalaBundle.message("local.project.libraries")

  override def buildJarStream(implicit indicator: ProgressIndicator): JStream[Path] = {
    if (contextDirectory == null)
      return JStream.empty()

    VfsUtilCore.virtualToIoFile(contextDirectory).toOption
      .map(_.toPath / "lib")
      .filter(_.exists)
      .map(collectJarFiles)
      .getOrElse(JStream.empty())
  }
}

