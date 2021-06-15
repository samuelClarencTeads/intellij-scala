package org.jetbrains.plugins.scala.testingSupport.test.utils

import com.intellij.execution.CantRunException
import com.intellij.execution.configurations.JavaParameters
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.jrt.JrtFileSystem
import org.jetbrains.plugins.scala.project.{ModuleExt, ProjectExt}

import scala.jdk.CollectionConverters.*

/**
 * (Hacky) Helper class needed to skip "build modules" classpath during tests run.
 * Example of "build module" is module created for sbt `project` folders (see any sbt project structure)
 * Such modules can contain Scala runtime libraries with version different from project version.
 * E.g. if you use sbt 1.3.x and scala 2.13.3, "build module" will use scala 2.12.x anyway
 */
private[test] class JavaParametersModified extends JavaParameters {

  import JavaParameters.*

  /**
   * copied from [[com.intellij.execution.configurations.JavaParameters]]
   * modified to include non-build modules classpath only
   */
  override def configureByProject(project: Project, classPathType: Int, jdk: Sdk): Unit = {
    if ((classPathType & JDK_ONLY) != 0) {
      if (jdk == null)
        throw CantRunException.noJdkConfigured
      setJdk(jdk)
    }

    if ((classPathType & CLASSES_ONLY) == 0)
      return

    setDefaultCharset(project)

    val nonBuildModules = project.modules.filterNot(_.isBuildModule)
    val nonBuildModulesRoots = ProjectRootManager.getInstance(project).orderEntries(nonBuildModules.asJavaCollection)
    configureEnumerator(nonBuildModulesRoots.runtimeOnly, classPathType, jdk).collectPaths(getClassPath)
  }

  private def configureEnumerator(enumerator0: OrderEnumerator, classPathType: Int, jdk: Sdk) = {
    var enumerator = enumerator0
    if ((classPathType & INCLUDE_PROVIDED) == 0) enumerator = enumerator.runtimeOnly
    if ((classPathType & JDK_ONLY) == 0) enumerator = enumerator.withoutSdk
    if ((classPathType & TESTS_ONLY) == 0) enumerator = enumerator.productionOnly

    var rootsEnumerator = enumerator.classes

    if ((classPathType & JDK_ONLY) != 0)
      rootsEnumerator = rootsEnumerator.usingCustomRootProvider { (e: OrderEntry) =>
        if (!e.isInstanceOf[JdkOrderEntry])
          e.getFiles(OrderRootType.CLASSES)
        else
          jdkRoots(jdk)
      }
    rootsEnumerator
  }

  private def jdkRoots(jdk: Sdk): Array[VirtualFile] = {
    val files = jdk.getRootProvider.getFiles(OrderRootType.CLASSES)
    files.filterNot(JrtFileSystem.isModuleRoot)
  }
}
