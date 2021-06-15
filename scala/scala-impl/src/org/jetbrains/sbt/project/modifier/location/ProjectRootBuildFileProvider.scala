package org.jetbrains.sbt.project.modifier.location

import java.io.File

import com.intellij.openapi.module.{Module as IJModule}
import org.jetbrains.sbt.project.modifier.BuildFileElementType

/**
 * @author Roman.Shein
 * @since 16.03.2015.
 */
object ProjectRootBuildFileProvider extends BuildFileProvider {
  override def findIoFile(module: IJModule, elementType: BuildFileElementType): Option[BuildFileEntry[File]] = {
    import org.jetbrains.sbt.*
    val project = module.getProject
    val buildFile = project.getBasePath.toFile / Sbt.BuildFile
    if (buildFile.exists) Some(BuildFileEntry(buildFile, isModuleLocal = false)) else None
  }
}
