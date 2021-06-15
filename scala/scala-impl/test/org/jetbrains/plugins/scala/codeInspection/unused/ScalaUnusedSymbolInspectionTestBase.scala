package org.jetbrains.plugins.scala.codeInspection.unused

import com.intellij.codeInspection.LocalInspectionTool
import org.jetbrains.plugins.scala.codeInspection.unusedInspections.ScalaUnusedSymbolInspection
import org.jetbrains.plugins.scala.codeInspection.{ScalaInspectionBundle, ScalaQuickFixTestBase}

abstract class ScalaUnusedSymbolInspectionTestBase extends ScalaQuickFixTestBase {

  override protected val classOfInspection: Class[? <: LocalInspectionTool] =
    classOf[ScalaUnusedSymbolInspection]

  override protected val description: String =
    ScalaUnusedSymbolInspection.annotationDescription

  val hint = ScalaInspectionBundle.message("remove.unused.element")
  val hintWholeDefinition = "Remove whole definition"
  val hintOnlyXBinding = "Remove only x binding"
}
