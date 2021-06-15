package org.jetbrains.bsp.project

import java.io.File
import java.nio.file.Paths
import java.util.Collections

import com.intellij.compiler.impl.{CompileContextImpl, CompilerUtil, ProjectCompileScope}
import com.intellij.compiler.progress.CompilerTask
import com.intellij.openapi.compiler.{CompilerPaths, CompilerTopics}
import com.intellij.openapi.externalSystem.model.project.{ExternalSystemSourceType, ModuleData}
import com.intellij.openapi.externalSystem.model.{DataNode, ProjectKeys}
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.{ExternalSystemApiUtil as ES}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.task.*
import org.jetbrains.bsp.data.BspMetadata
import org.jetbrains.bsp.project.BspTask.BspTarget
import org.jetbrains.bsp.project.test.BspTestRunConfiguration
import org.jetbrains.bsp.{BSP, BspBundle, BspUtil}
import org.jetbrains.concurrency.{AsyncPromise, Promise}
import org.jetbrains.plugins.scala.build.BuildMessages
import org.jetbrains.plugins.scala.extensions

import scala.jdk.CollectionConverters.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


class BspProjectTaskRunner extends ProjectTaskRunner {

  override def canRun(projectTask: ProjectTask): Boolean = projectTask match {
    case task: ModuleBuildTask =>
      val module = task.getModule
      val moduleType = ModuleType.get(module)
      moduleType match {
        case _ => BspUtil.isBspModule(module)
      }
    case t: ExecuteRunConfigurationTask => t.getRunProfile match {
      case _: BspTestRunConfiguration => true
      case _ => false
    }
    case _ => false
  }


  override def run(project: Project,
                   projectTaskContext: ProjectTaskContext,
                   tasks: ProjectTask*): Promise[ProjectTaskRunner.Result] = {

    val validTasks = tasks.collect {
      case task: ModuleBuildTask => task
    }

    val targetsAndRebuild = validTasks.flatMap { task =>
      val moduleId = ES.getExternalProjectId(task.getModule)

      // TODO all these Options fail silently. collect errors and report something
      val targetIds = for {
        projectPath <- Option(ES.getExternalProjectPath(task.getModule))
        projectData <- Option(ES.findProjectData(project, BSP.ProjectSystemId, projectPath))
        moduleDataNode <- Option(ES.find(
          projectData, ProjectKeys.MODULE,
          (node: DataNode[ModuleData]) => node.getData.getId == moduleId))
        metadata <- Option(ES.find(moduleDataNode, BspMetadata.Key))
      } yield {
        val data = metadata.getData
        val workspaceUri = Paths.get(projectPath).toUri
        data.targetIds.asScala.map(id => BspTarget(workspaceUri, id)).toList
      }

      targetIds.getOrElse(List.empty)
        .map(id => (id, ! task.isIncrementalBuild))
    }

    val targets = targetsAndRebuild.map(_._1)
    val targetsToClean = targetsAndRebuild.filter(_._2).map(_._1)

    // TODO save only documents in affected targets?
    extensions.invokeAndWait {
      FileDocumentManager.getInstance().saveAllDocuments()
    }
    val promiseResult = new AsyncPromise[ProjectTaskRunner.Result]

    val bspTask = new BspTask(project, targets, targetsToClean)

    bspTask.resultFuture.foreach { _ =>
        val modules = validTasks.map(_.getModule).toArray
        val outputRoots = CompilerPaths.getOutputPaths(modules)
        refreshRoots(project, outputRoots)
      }

    bspTask.resultFuture.onComplete { messages =>

      val session = new CompilerTask(project, BspBundle.message("bsp.runner.hack.notify.completed.bsp.build"), false, false, false, false)
      val scope = new ProjectCompileScope(project)
      val context = new CompileContextImpl(project, session, scope, false, false)
      val pub = project.getMessageBus.syncPublisher(CompilerTopics.COMPILATION_STATUS)

      messages match {
        case Success(messages) =>
          promiseResult.setResult(messages.toTaskRunnerResult)

          // Auto-test needs checks if at least one path was process (and this can be any path)
          pub.fileGenerated("", "")
          pub.compilationFinished(
            messages.status == BuildMessages.Canceled,
            messages.errors.size, messages.warnings.size, context)
        case Failure(exception) =>
          promiseResult.setError(exception)
          pub.automakeCompilationFinished(1, 0, context)
      }
    }


    ProgressManager.getInstance().run(bspTask)

    promiseResult
  }

  // remove this if/when external system handles this refresh on its own
  private def refreshRoots(project: Project, outputRoots: Array[String]): Unit = {

    // simply refresh all the source roots to catch any generated files
    val info = Option(ProjectDataManager.getInstance().getExternalProjectData(project, BSP.ProjectSystemId, project.getBasePath))
    val allSourceRoots = info
      .map { info => ES.findAllRecursively(info.getExternalProjectStructure, ProjectKeys.CONTENT_ROOT) }
      .getOrElse(Collections.emptyList())
    val generatedSourceRoots = allSourceRoots.asScala.flatMap { node =>
      val data = node.getData
      // bsp-side generated sources are still imported as regular sources
      val generated = data.getPaths(ExternalSystemSourceType.SOURCE_GENERATED).asScala
      val regular = data.getPaths(ExternalSystemSourceType.SOURCE).asScala
      generated ++ regular
    }.map(_.getPath).toSeq.distinct

    val toRefresh = generatedSourceRoots ++ outputRoots

    CompilerUtil.refreshOutputRoots(toRefresh.asJavaCollection)
    val toRefreshFiles = toRefresh.map(new File(_)).asJava
    LocalFileSystem.getInstance().refreshIoFiles(toRefreshFiles, true, true, null)
  }
}
