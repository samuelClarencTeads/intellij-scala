package org.jetbrains.plugins.scala.testingSupport.test

import com.intellij.execution.configuration.RunConfigurationExtensionsManager
import com.intellij.execution.configurations.{CommandLineState, RunConfigurationBase}
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.testDiscovery.JavaAutoRunManager
import com.intellij.execution.testframework.autotest.{AbstractAutoTestManager, ToggleAutoTestAction}
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.{DefaultExecutionResult, JavaRunConfigurationExtensionManager}
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.testingSupport.test.actions.ScalaRerunFailedTestsAction
import org.jetbrains.plugins.scala.testingSupport.test.exceptions.executionException
import org.jetbrains.plugins.scala.testingSupport.test.testdata.TestConfigurationData

@ApiStatus.Internal
trait ScalaTestFrameworkCommandLineStateLike {
  self: CommandLineState =>

  def configuration: AbstractTestRunConfiguration

  def failedTests: Option[Seq[(String, String)]]

  protected implicit val module: Module =
    Option(configuration.getModule).getOrElse(
      throw executionException(ScalaBundle.message("test.run.config.module.is.not.specified"))
    )

  protected val project: Project = module.getProject

  protected val testConfigurationData: TestConfigurationData = configuration.testConfigurationData

  protected def buildSuitesToTestsMap: Map[String, Set[String]] =
    failedTests match {
      case Some(failed) =>
        val grouped = failed.groupBy(_._1).map { case (aClass, tests) => (aClass, tests.map(_._2).toSet) }
        grouped.filter(_._2.nonEmpty)
      case None =>
        testConfigurationData.getTestMap
    }

  protected final object DebugOptions {
    // set to true to debug test runner process
    def attachDebugAgent = false && !ApplicationManager.getApplication.isUnitTestMode
    def waitUntilDebuggerAttached = true
  }

  protected def createExecutionResult(
    consoleView: ConsoleView,
    processHandler: ProcessHandler
  ): DefaultExecutionResult = {
    val result = new DefaultExecutionResult(consoleView, processHandler)
    val restartActions = createRestartActions(consoleView)
    result.setRestartActions(restartActions*)
    result
  }

  protected def createRestartActions(consoleView: ConsoleView): Seq[AnAction] =
    consoleView match {
      case testConsole: BaseTestsOutputConsoleView =>
        val rerunFailedTestsAction = {
          val properties = testConsole.getProperties.asInstanceOf[ScalaTestFrameworkConsoleProperties]
          val action = new ScalaRerunFailedTestsAction(testConsole, properties)
          action.setModelProvider(() => testConsole.asInstanceOf[SMTRunnerConsoleView].getResultsViewer)
          action
        }

        val toggleAutoTestAction = new ToggleAutoTestAction() {
          override def isDelayApplicable: Boolean = false
          override def getAutoTestManager(project: Project): AbstractAutoTestManager = JavaAutoRunManager.getInstance(project)
        }
        Seq(rerunFailedTestsAction, toggleAutoTestAction)
      case _ =>
        Nil
    }

  protected def attachExtensionsToProcess(
    configuration: RunConfigurationBase[?],
    processHandler: ProcessHandler
  ): Unit = {
    val runnerSettings = getRunnerSettings
    configurationExtensionManager.attachExtensionsToProcess(configuration, processHandler, runnerSettings)
  }

  // case is required to avoid bad red-highlighting by Scala Plugin which can't understand Kotlin generics
  private def configurationExtensionManager: RunConfigurationExtensionsManager[RunConfigurationBase[?], ?] =
    JavaRunConfigurationExtensionManager.getInstance.asInstanceOf[RunConfigurationExtensionsManager[RunConfigurationBase[?], ?]]
}


