package org.jetbrains.plugins.scala.runner

import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.{JavaParameters, ParametersList, RunConfigurationBase, RunnerSettings}
import com.intellij.execution.{ExecutionException, RunConfigurationExtension}
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.runner.Scala3MainMethodSyntheticClass.MainMethodParameters
import org.jetbrains.plugins.scala.runner.view.ScalaProvideMainMethodParametersDialog

import scala.jdk.CollectionConverters.ListHasAsScala

final class ScalaApplicationConfigurationExtension extends RunConfigurationExtension {

  import ValidateCommand.*

  override def isApplicableFor(
    configuration: RunConfigurationBase[?]
  ): Boolean =
    configuration.isInstanceOf[ApplicationConfiguration]

  /**
   * NOTE: Unfortunately it this method can't be used to show a warning in configuration ui,
   * even if you throw RuntimeConfigurationWarning. This method is called in
   * [[com.intellij.execution.JavaRunConfigurationExtensionManager.checkConfigurationIsValid()]],
   * where exceptions are intercepted, and just logged.
   *
   * @see [[com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl.checkSettings]]
   */
  override def validateConfiguration(
    configurationBase: RunConfigurationBase[?],
    isExecution: Boolean
  ): Unit = {}

  @throws[ExecutionException]
  override def updateJavaParameters[T <: RunConfigurationBase[?]](
    configurationBase: T,
    javaParams: JavaParameters,
    runnerSettings: RunnerSettings
  ): Unit = {
    val configuration = configurationBase.asOptionOfUnsafe[ApplicationConfiguration].getOrElse(return)

    configuration.getMainClass match {
      case clazz: Scala3MainMethodSyntheticClass =>
        clazz.parameters match {
          case MainMethodParameters.Custom(expectedParams) if expectedParams.nonEmpty =>
            ensureAllCustomParameterValuesProvided(configuration, javaParams, expectedParams)
          case _ =>
        }
      case _ =>
    }
  }

  /** modifies original configuration parameters if they were provided via dialog */
  private def ensureAllCustomParameterValuesProvided(
    configuration: ApplicationConfiguration,
    javaParameters: JavaParameters,
    expectedParams: Seq[MainMethodParameters.CustomParameter]
  ): Unit = {
    assert(expectedParams.nonEmpty)

    val programParametersList = javaParameters.getProgramParametersList
    val command = validateAllCustomParameterValuesAreProvided(
      configuration.getProject,
      programParametersList.getParameters.asScala.toSeq,
      expectedParams
    )
    command match {
      case DoNothing =>
      case CancelExecution =>
        throw new ExecutionException(ScalaBundle.message("execution.cancelled"))
      case UpdateParameters(newParameters) =>
        programParametersList.clearAll()
        programParametersList.addAll(newParameters*)

        configuration.setProgramParameters(programParametersList.getParametersString)
    }
  }

  /**
   * Validates that application configuration program parameters contain enough values to match
   * all custom parameters of scala3 @main method (in case non-standard parameters (`args: String*`) are used).
   *
   * If provided arguments are not enough, it shows a dialog which allows user filling missing values for the parameters.
   */
  @RequiresEdt
  private def validateAllCustomParameterValuesAreProvided(
    project: Project,
    actualParameters: Seq[String],
    expectedParameters: Seq[MainMethodParameters.CustomParameter]
  ): ValidationResultCommand = {
    val expectedCount = expectedParameters.size
    val actualCount = actualParameters.size

    val hasVarargs = expectedParameters.last.isVararg
    val tooFewArguments =
      if (hasVarargs) actualCount < expectedCount - 1 // vararg parameter can contain zero elements
      else actualCount < expectedCount

    if (tooFewArguments) {
      val dialog = new ScalaProvideMainMethodParametersDialog(project, expectedParameters, actualParameters)
      val ok = dialog.showAndGet()
      if (ok) {
        val argumentsFromForm = dialog.filledParametersValues
        val argumentsFinal = fixVarargParameter(argumentsFromForm, hasVarargs)
        UpdateParameters(argumentsFinal)
      }
      else CancelExecution
    }
    else DoNothing
  }

  /**
   * When the last parameter is vararg, we imply that form input can contain several values.
   *  - If vararg parameter type is `Int*` and user input is `1 2 3`, actual parameters will be Seq(1, 2, 3)
   *  - If vararg parameter type is `String*` and user input is `one two three`, actual parameters will be Seq("one", "two", "three")
   *  - If vararg parameter type is `String*` and user input is `"one two three"`, actual parameters will be Seq("one two three")
   *    (If in this case user wants to input string with spaces s/he needs to escape them manually with double quotes ("))
   *
   * All other, non-vararg parameters are escaped automatically:
   *  - If the parameter value is `42 hello world` then it will be passed as a single string argument with escaped spaces
   */
  private def fixVarargParameter(argumentsFromForm: Seq[String], hasVarargs: Boolean): Seq[String] =
    if (hasVarargs) {
      val (init, last) = (argumentsFromForm.init, argumentsFromForm.last)
      val lastActual = ParametersList.parse(last).toSeq
      init ++ lastActual
    }
    else {
      argumentsFromForm
    }

  private sealed trait ValidationResultCommand
  private object ValidateCommand {
    object DoNothing extends ValidationResultCommand
    object CancelExecution extends ValidationResultCommand
    case class UpdateParameters(parameters: Seq[String]) extends ValidationResultCommand
  }
}