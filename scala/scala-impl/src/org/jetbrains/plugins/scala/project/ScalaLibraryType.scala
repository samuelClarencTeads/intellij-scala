package org.jetbrains.plugins.scala
package project

import java.io.File
import java.{util as ju}

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.*
import com.intellij.openapi.roots.ui.configuration.*
import com.intellij.openapi.roots.{JavadocOrderRootType, OrderRootType}
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.{Icon, JComponent}
import org.jetbrains.plugins.scala.ScalaBundle

/**
 * @author Pavel Fatin
 */
final class ScalaLibraryType extends LibraryType[ScalaLibraryProperties](ScalaLibraryType.Kind) {

  override def getIcon(properties: ScalaLibraryProperties): Icon = icons.Icons.SCALA_SDK

  override def getCreateActionName: String = ScalaBundle.message("library.type.scala.sdk")

  override def createNewLibrary(parent: JComponent,
                                contextDirectory: VirtualFile,
                                project: Project): NewLibraryConfiguration =
    ScalaLibraryType.Description.createNewLibrary(parent, contextDirectory)

  //noinspection TypeAnnotation
  override def createPropertiesEditor(editorComponent: ui.LibraryEditorComponent[ScalaLibraryProperties]) =
    new ui.LibraryPropertiesEditor {

      private val form = new ScalaLibraryEditorForm()

      override def createComponent: JComponent = form.getComponent

      override def isModified: Boolean = formState != propertiesState

      override def apply(): Unit = {
        properties.loadState(formState)
      }

      override def reset(): Unit = {
        val state = propertiesState
        form.setLanguageLevel(state.getLanguageLevel)
        form.setClasspath(state.getCompilerClasspath)
      }

      private def properties = editorComponent.getProperties

      private def propertiesState = properties.getState

      private def formState = new ScalaLibraryPropertiesState(
        form.getLanguageLevel,
        form.getClasspath
      )
    }

}

//noinspection TypeAnnotation
object ScalaLibraryType {

  import template.*

  def apply() =
    LibraryType.findByKind(Kind).asInstanceOf[ScalaLibraryType]

  object Kind extends PersistentLibraryKind[ScalaLibraryProperties]("Scala") {

    override def createDefaultProperties() = ScalaLibraryProperties()
  }

  object Description extends libraries.CustomLibraryDescription {

    override def getSuitableLibraryKinds = ju.Collections.singleton(Kind)

    override def createNewLibrary(parent: JComponent,
                                  contextDirectory: VirtualFile) = {
      val sdkSelectionDialog = new SdkSelectionDialog(parent, contextDirectory)
      sdkSelectionDialog
        .open()
        .map(createNewScalaLibrary)
        .orNull
    }

    override def getDefaultLevel = projectRoot.LibrariesContainer.LibraryLevel.GLOBAL

    private def createNewScalaLibrary(descriptor: ScalaSdkDescriptor) = {
      val ScalaSdkDescriptor(version, compilerClasspath, libraryFiles, sourceFiles, docFiles) = descriptor

      new NewLibraryConfiguration(
        "scala-sdk-" + version.getOrElse(Version.Default),
        ScalaLibraryType(),
        ScalaLibraryProperties(version, compilerClasspath)
      ) {
        override def addRoots(editor: libraryEditor.LibraryEditor): Unit = {
          addRootsInner(libraryFiles, OrderRootType.CLASSES)(editor)
          addRootsInner(sourceFiles, OrderRootType.SOURCES)(editor)
          addRootsInner(docFiles, JavadocOrderRootType.getInstance)(editor)

          if (sourceFiles.isEmpty && docFiles.isEmpty) editor.addRoot(
            s"https://www.scala-lang.org/api/${version.getOrElse("current")}/",
            JavadocOrderRootType.getInstance
          )
        }

        private def addRootsInner(files: Iterable[File], rootType: OrderRootType)
                                 (implicit editor: libraryEditor.LibraryEditor): Unit =
          for {
            file <- files
            url = file.toLibraryRootURL
          } editor.addRoot(url, rootType)
      }
    }
  }

}