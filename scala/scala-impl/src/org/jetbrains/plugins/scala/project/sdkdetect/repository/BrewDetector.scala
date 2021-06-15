package org.jetbrains.plugins.scala.project.sdkdetect.repository

import java.nio.file.{Path, Paths}
import java.util.function.{Function as JFunction}
import java.util.stream.{Stream as JStream}

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.SystemInfo
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.project.template.{PathExt, *}

private[repository] object BrewDetector extends ScalaSdkDetector {
  override def buildSdkChoice(descriptor: ScalaSdkDescriptor): SdkChoice = BrewSdkChoice(descriptor)
  override def friendlyName: String = ScalaBundle.message("brew.packages")

  override def buildJarStream(implicit indicator: ProgressIndicator): JStream[Path] = {
    val scalaRoot = Paths.get("/") / "usr" / "local" / "Cellar" / "scala"

    if (!SystemInfo.isMac || !scalaRoot.exists)
      return JStream.empty()

    scalaRoot.children
      .filter(f => (f / "libexec" / "lib").exists)
      .map[JStream[Path]](collectJarFiles)
      .flatMap(JFunction.identity[JStream[Path]]())

  }
}