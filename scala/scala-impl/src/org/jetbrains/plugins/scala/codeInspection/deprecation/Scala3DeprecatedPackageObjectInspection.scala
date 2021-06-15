package org.jetbrains.plugins.scala.codeInspection.deprecation

import com.intellij.codeInspection.{InspectionManager, LocalQuickFix, ProblemDescriptor, ProblemHighlightType}
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.codeInsight.unwrap.{ScalaUnwrapContext, ScalaUnwrapper}
import org.jetbrains.plugins.scala.codeInspection.{
  getActiveEditor,
  AbstractFixOnPsiElement,
  AbstractRegisteredInspection,
  ScalaInspectionBundle
}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt

class Scala3DeprecatedPackageObjectInspection extends AbstractRegisteredInspection {
  import Scala3DeprecatedPackageObjectInspection.*

  override protected def problemDescriptor(
    element:             PsiElement,
    maybeQuickFix:       Option[LocalQuickFix],
    descriptionTemplate: String,
    highlightType:       ProblemHighlightType
  )(implicit
    manager:    InspectionManager,
    isOnTheFly: Boolean
  ): Option[ProblemDescriptor] =
    element match {
      case obj: ScObject if obj.isPackageObject && obj.isInScala3Module =>
        super.problemDescriptor(
          obj.nameId,
          unwrapPackageObjectQuickFix(obj),
          message,
          ProblemHighlightType.LIKE_DEPRECATED
        )
      case _ => None
    }
}

object Scala3DeprecatedPackageObjectInspection {
  private[deprecation] val message = ScalaInspectionBundle.message("package.objects.are.deprecated")
  private[deprecation] val fixId   = ScalaInspectionBundle.message("unwrap.package.object.fix")
  private val unwrapper            = new PackageObjectUnwrapper

  private def unwrapPackageObjectQuickFix(obj: ScObject): Option[LocalQuickFix] =
    Option.when(obj.extendsBlock.templateParents.forall(_.typeElements.isEmpty))(
      new AbstractFixOnPsiElement[ScObject](fixId, obj) {
        override protected def doApplyFix(element: ScObject)(implicit project: Project): Unit =
          getActiveEditor(element).foreach(unwrapper.unwrap(_, element))
      }
    )

  private class PackageObjectUnwrapper extends ScalaUnwrapper {
    override def isApplicableTo(e: PsiElement): Boolean = true

    override def doUnwrap(element: PsiElement, context: ScalaUnwrapContext): Unit = {
      context.extractAllMembers(element.asInstanceOf[ScObject])
      context.deleteExactly(element)
    }
  }
}
