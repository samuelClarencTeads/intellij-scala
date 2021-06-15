package org.jetbrains.plugins.scala.codeInsight.hints

import java.{util as ju}

import com.intellij.codeInsight.hints.settings.{InlayProviderSettingsModel, InlaySettingsProvider}
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.ScalaLanguage
import org.jetbrains.plugins.scala.codeInsight.hints.methodChains.ScalaMethodChainInlayHintsSettingsModel

class ScalaTypeHintsSettingsProvider extends InlaySettingsProvider {
  override def createModels(project: Project, language: Language): ju.List[InlayProviderSettingsModel] =
    if (language == ScalaLanguage.INSTANCE) ju.Arrays.asList(
      new ScalaTypeHintsSettingsModel(project),
      new TypeMismatchHintsSettingsModel(project),
      new ScalaMethodChainInlayHintsSettingsModel(project),
      new ScalaGeneralTypeHintsSettingsModel
    )
    else ju.Collections.emptyList()

  override def getSupportedLanguages(project: Project): ju.Collection[Language] =
    ju.Collections.singletonList(ScalaLanguage.INSTANCE)
}
