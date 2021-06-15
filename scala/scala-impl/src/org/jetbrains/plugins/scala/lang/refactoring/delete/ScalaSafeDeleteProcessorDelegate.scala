package org.jetbrains.plugins.scala
package lang
package refactoring
package delete

import java.util.{List as JList}

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.psi.*
import com.intellij.refactoring.safeDelete.{JavaSafeDeleteProcessor, NonCodeUsageSearchInfo}
import com.intellij.usageView.UsageInfo
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.refactoring.delete.SafeDeleteProcessorUtil.*

class ScalaSafeDeleteProcessorDelegate extends JavaSafeDeleteProcessor {
  override def handlesElement(element: PsiElement): Boolean =
    element.containingScalaFile.isDefined && super.handlesElement(element)

  @Nullable
  override def findUsages(element: PsiElement, allElementsToDelete: Array[PsiElement], usages: JList[UsageInfo]): NonCodeUsageSearchInfo = {
    var insideDeletedCondition: Condition[PsiElement] = getUsageInsideDeletedFilter(allElementsToDelete)

    insideDeletedCondition = element match {
      case c: PsiTypeParameter =>
        findClassUsages(c, allElementsToDelete, usages)
        findTypeParameterExternalUsages(c, usages)
        insideDeletedCondition
      case c: ScTypeDefinition =>
        findClassUsages(c, allElementsToDelete, usages)
        insideDeletedCondition
      case m: ScFunction => // TODO Scala specific override/implements, extend to vals, members, type aliases etc.
        findMethodUsages(m, allElementsToDelete, usages)
      case param: ScParameter =>
        findParameterUsages(param, usages)
        insideDeletedCondition
      case _ =>
        insideDeletedCondition
    }

    new NonCodeUsageSearchInfo(insideDeletedCondition, element)
  }

  override def preprocessUsages(project: Project, usages: Array[UsageInfo]): Array[UsageInfo] = {
    // Currently this ProcessorDelegate emits SafeDeleteReferenceJavaDeleteUsageInfo, which gets processed by the JavaSafeDeleteProcessor.
    // Right now we rely on that processor to preprocess the usages, so we intentionally don't call super here!

    // TODO Use Scala specific DeleteUsageInfo, and consider them here.
    usages
  }
}
