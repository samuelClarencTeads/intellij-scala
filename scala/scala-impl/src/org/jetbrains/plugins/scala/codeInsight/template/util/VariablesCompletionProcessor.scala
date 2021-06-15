package org.jetbrains.plugins.scala
package codeInsight.template.util

import com.intellij.psi.*
import org.jetbrains.plugins.scala.lang.resolve.processor.BaseProcessor
import org.jetbrains.plugins.scala.lang.resolve.{ResolveTargets, ScalaResolveResult}
import org.jetbrains.plugins.scala.project.ProjectContext

/**
 * User: Alexander Podkhalyuzin
 * Date: 30.01.2009
 */
class VariablesCompletionProcessor(override val kinds: Set[ResolveTargets.Value])
                                  (implicit ctx: ProjectContext) extends BaseProcessor(kinds) {

  override protected def execute(namedElement: PsiNamedElement)
                                (implicit state: ResolveState): Boolean = {
    candidatesSet = candidatesSet union Set(new ScalaResolveResult(namedElement))
    true
  }
}