package org.jetbrains.plugins.scala.packagesearch

import com.intellij.buildsystem.model.unified.UnifiedDependency
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.{DumbService, Project}
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.{Navigatable, NavigatableAdapter}
import com.jetbrains.packagesearch.intellij.plugin.extensibility.{ModuleTransformer, ProjectModule}
import com.jetbrains.packagesearch.intellij.plugin.ui.toolwindow.models.PackageVersion
import org.jetbrains.plugins.scala.packagesearch.utils.{SbtCommon, SbtDependencyUtils, SbtProjectModuleType, ScalaKotlinHelper}
import org.jetbrains.sbt.SbtUtil

import java.io.File
import java.util
import scala.jdk.CollectionConverters.*

class SbtModuleTransformer(private val project: Project) extends ModuleTransformer {

  def findModulePaths(module: Module): Array[File] = {
    if (!SbtUtil.isSbtModule(module)) return null
    val contentRoots = ModuleRootManager.getInstance(module).getContentRoots
    if (contentRoots.length < 1) return null
    contentRoots.map(virtualFile => {
      new File(virtualFile.getPath)
    })
  }

  def createNavigableDependencyCallback(project: Project,
                                        module: Module): (String, String, PackageVersion) =>
    Navigatable = (groupId: String, artifactId: String, packageVersion: PackageVersion) => {

    val targetedLibDep = SbtDependencyUtils.findLibraryDependency(
      project,
      module,
      new UnifiedDependency(groupId, artifactId, packageVersion.toString, SbtCommon.defaultLibScope),
      configurationRequired = false
    )

    new NavigatableAdapter() {
      override def navigate(requestFocus: Boolean): Unit = {
        PsiNavigationSupport.getInstance.createNavigatable(
          project,
          targetedLibDep._1.getContainingFile.getVirtualFile,
          targetedLibDep._1.getTextOffset
        ).navigate(requestFocus)
      }
    }
  }

  def obtainProjectModulesFor(module: Module):ProjectModule = try {
    val sbtFileOpt = SbtDependencyUtils.getSbtFileOpt(module)
    sbtFileOpt match {
      case Some(buildFile: VirtualFile) =>
          val projectModule = new ProjectModule(
            module.getName,
            module,
            null,
            buildFile,
            SbtCommon.buildSystemType,
            SbtProjectModuleType,
            (_,_,_) => null
        )
        if (!DumbService.getInstance(project).isDumb)
          ScalaKotlinHelper.createNavigatableProjectModule(projectModule, createNavigableDependencyCallback(project, module))
        else
          projectModule
      case _ => null
    }

  } catch {
    case e: Exception => null
  }

  override def transformModules(list: util.List[? <: Module]): util.List[ProjectModule] = {
//    if (DumbService.getInstance(project).isDumb) return List.empty.asJava
    list.asScala.map(module => obtainProjectModulesFor(module)).filter(_ != null).distinct.asJava
  }

  // TODO remove this function from the interface when needed
  override def transformModules(modules: Array[Module]): util.List[ProjectModule] = {
    transformModules(modules.toList.asJava)
  }
}
