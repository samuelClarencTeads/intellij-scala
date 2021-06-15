package org.jetbrains.plugins.scala
package findUsages
package setter

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.search.{PsiSearchHelper, SearchScope, TextOccurenceProcessor, UsageSearchContext}
import com.intellij.util.{Processor, QueryExecutor}
import org.jetbrains.plugins.scala.extensions.{&&, Parent, inReadAction}
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil.*
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScReferencePattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScAssignment
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScVariable}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.fake.FakePsiMethod
import org.jetbrains.plugins.scala.lang.psi.light.PsiTypedDefinitionWrapper


class SetterMethodSearcher extends QueryExecutor[PsiReference, ReferencesSearch.SearchParameters] {
  private val suffixScala = "_="
  private val suffixJava = "_$eq"

  override def execute(queryParameters: ReferencesSearch.SearchParameters, cons: Processor[? >: PsiReference]): Boolean = {
    implicit val scope: SearchScope = inReadAction(queryParameters.getEffectiveSearchScope)
    implicit val consumer: Processor[? >: PsiReference] = cons
    val element = queryParameters.getElementToSearch
    val project = queryParameters.getProject
    val data: Option[(ScNamedElement, String)] = inReadAction {
      element match {
        case _ if !element.isValid => None
        case f: ScFunction => Some((f, f.name))
        case (rp: ScReferencePattern) && inNameContext(_: ScVariable) => Some((rp, rp.name))
        case _ => None
      }
    }
    data match {
      case Some((fun: ScFunction, name)) if name endsWith suffixScala =>
        processAssignments(fun, name, project)
        processSimpleUsages(fun, name, project)
      case Some((pattern: ScReferencePattern, name)) =>
        processAssignments(pattern, name, project)
        processSimpleUsages(pattern, name + suffixScala, project)
        processSimpleUsages(pattern, name + suffixJava, project)
      case _ => true
    }
  }

  private def processAssignments(element: PsiElement, name: String, project: Project)
                                (implicit consumer: Processor[? >: PsiReference], scope: SearchScope): Boolean = {
    val processor = new TextOccurenceProcessor {
      override def execute(elem: PsiElement, offsetInElement: Int): Boolean = {
        inReadAction {
          elem match {
            case Parent(Parent(assign: ScAssignment)) => assign.resolveAssignment match {
              case Some(res) if res.element.getNavigationElement == element =>
                Option(assign.leftExpression).foreach {
                  case ref: ScReference => if (!consumer.process(ref)) return false
                }
              case _ =>
            }
            case _ =>
          }
          true
        }
      }
    }
    val helper: PsiSearchHelper = PsiSearchHelper.getInstance(project)
    helper.processElementsWithWord(processor, scope, name.stripSuffix(suffixScala), UsageSearchContext.IN_CODE, true)
  }

  private def processSimpleUsages(element: PsiElement, name: String, project: Project)
                                 (implicit consumer: Processor[? >: PsiReference], scope: SearchScope): Boolean = {
    val processor = new TextOccurenceProcessor {
      override def execute(elem: PsiElement, offsetInElement: Int): Boolean = {
        inReadAction {
          elem match {
            case ref: PsiReference => ref.resolve() match {
              case FakePsiMethod(`element`) => if (!consumer.process(ref)) return false
              case PsiTypedDefinitionWrapper(`element`) => if (!consumer.process(ref)) return false
              case _ =>
            }
            case _ =>
          }
        }
        true
      }
    }
    val helper: PsiSearchHelper = PsiSearchHelper.getInstance(project)
    helper.processElementsWithWord(processor, scope, name, UsageSearchContext.IN_CODE, true)
  }
}
