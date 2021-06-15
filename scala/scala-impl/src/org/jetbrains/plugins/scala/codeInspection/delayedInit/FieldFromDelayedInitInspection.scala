package org.jetbrains.plugins.scala
package codeInspection
package delayedInit

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScPatternDefinition, ScValueOrVariable, ScVariableDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScObject, ScTemplateDefinition}
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult

import scala.annotation.nowarn

/**
  * @author Nikolay.Tropin
  */
@nowarn("msg=" + AbstractInspection.DeprecationText)
final class FieldFromDelayedInitInspection extends AbstractInspection(ScalaInspectionBundle.message("display.name.field.from.delayedinit")) {

  import FieldFromDelayedInitInspection.*

  override protected def actionFor(implicit holder: ProblemsHolder, isOnTheFly: Boolean): PartialFunction[PsiElement, Any] = {
    case ref: ScReferenceExpression =>
      for {
        case FieldInDelayedInit(delayedInitClass) <- ref.bind()
        parents = parentDefinitions(ref)
        if !parents.exists(_.sameOrInheritor(delayedInitClass))
      } holder.registerProblem(ref.nameId, ScalaInspectionBundle.message("field.defined.in.delayedinit.is.likely.to.be.null"))
  }
}

object FieldFromDelayedInitInspection {

  private object FieldInDelayedInit {

    def unapply(result: ScalaResolveResult): Option[ScTemplateDefinition] =
      result.fromType.flatMap { scType =>
        ScalaPsiUtil.nameContext(result.getElement) match {
          case LazyVal(_) => None
          case definition@(_: ScPatternDefinition | _: ScVariableDefinition) =>
            Option(definition.asInstanceOf[ScValueOrVariable].containingClass).collect {
              case scalaClass: ScClass => scalaClass
              case scalaObject: ScObject => scalaObject
            }.filter(conformsToTypeFromClass(scType, "scala.DelayedInit")(_))
          case _ => None
        }
      }
  }

  private def parentDefinitions(reference: ScReferenceExpression) =
    reference.parentsInFile.collect {
      case definition: ScTemplateDefinition => definition
    }
}
