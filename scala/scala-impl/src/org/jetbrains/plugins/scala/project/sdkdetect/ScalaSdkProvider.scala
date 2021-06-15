package org.jetbrains.plugins.scala.project.sdkdetect

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.scala.ScalaBundle.message
import org.jetbrains.plugins.scala.project.sdkdetect.ScalaSdkProvider.Log
import org.jetbrains.plugins.scala.project.sdkdetect.repository.ScalaSdkDetector
import org.jetbrains.plugins.scala.project.sdkdetect.repository.ScalaSdkDetector.ExtraCompilerPathResolveFailure
import org.jetbrains.plugins.scala.project.template.{ScalaSdkComponent, ScalaSdkDescriptor, SdkChoice}

import java.util.function.Consumer
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.*

// TODO: use Java -> Scala stream converters from Scala 2.13
class ScalaSdkProvider(implicit indicator: ProgressIndicator, contextDirectory: VirtualFile) {

  protected val scalaJarDetectors: Seq[ScalaSdkDetector] = ScalaSdkDetector.allDetectors(contextDirectory)

  def discoverSDKs(callback: Consumer[SdkChoice]): Unit =
    scalaJarDetectors.foreach { detector =>
      indicator.setText(message("sdk.scan.title", detector.friendlyName))
      indicator.setIndeterminate(true)

      val jarStream = detector.buildJarStream
      val components = try {
        jarStream
          .iterator().asScala
          .map { f =>
            indicator.checkCanceled()
            //noinspection ReferencePassedToNls
            indicator.setText2(f.toString)
            val sdkComponent = ScalaSdkComponent.fromFile(f.toFile)
            sdkComponent.orNull
          }
          .filter(_ != null)
          .toSeq
      } finally {
        jarStream.close()
      }

      val componentsByVersion: Seq[(Option[String], Seq[ScalaSdkComponent])] =
        components.groupBy(_.version).to(ArraySeq)

      val sdkDescriptors: Seq[(Option[String], Either[Seq[String], ScalaSdkDescriptor])] = componentsByVersion.map { case (version, components) =>
        val descriptors = ScalaSdkDescriptor.buildFromComponents(components).left.map(e => Seq(e.nls))
        val descriptorsWithExtraJars = descriptors.flatMap(detector.resolveExtraRequiredJars(_).left.map(_.map(resolveErrorMessage)))
        (version, descriptorsWithExtraJars)
      }

      sdkDescriptors.foreach {
        case (version, Left(errors)) =>
          Log.trace(
            s"""Scala SDK Descriptor candidate is skipped (detector: ${detector.getClass.getSimpleName}, scalaVersion: $version)
               |${errors.mkString("\n")}""".stripMargin
          )
        case _ =>
      }

      val sdkChaisesSorted = sdkDescriptors
        .collect { case (Some(_), Right(descriptor)) => detector.buildSdkChoice(descriptor) }
        .sortBy(_.sdk.version)
        .reverse

      sdkChaisesSorted.foreach(callback.accept)
    }


  private def resolveErrorMessage(error: ExtraCompilerPathResolveFailure): String = {
    import ExtraCompilerPathResolveFailure.*
    val Prefix = "Compiler classpath resolve failure."
    error match {
      case NotSupportedForScalaVersion(scalaVersion) => s"$Prefix Not supported for scala version: $scalaVersion"
      case UnresolvedArtifact(artifactName)          => s"$Prefix Unresolved dependency: $artifactName"
      case AmbiguousArtifactsResolved(fileNames)     => s"$Prefix Ambiguous artifact resolved: ${fileNames.mkString(", ")}"
      case UnknownResolveProblem(resolveProblems)    => s"$Prefix Unknown resolve problems: ${resolveProblems.zipWithIndex.mkString(", ")}"
      case UnknownException(exception)               => s"$Prefix Unknown exception occurred: ${exception.getMessage}"
    }
  }
}

object ScalaSdkProvider {

  private val Log = Logger.getInstance(classOf[ScalaSdkProvider])
}
