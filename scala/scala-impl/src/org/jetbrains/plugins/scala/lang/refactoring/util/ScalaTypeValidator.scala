package org.jetbrains.plugins.scala.lang.refactoring.util

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTreeUtil.getParentOfType
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.statements.*
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParam
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.resolve.ResolveTargets.*
import org.jetbrains.plugins.scala.lang.resolve.processor.BaseProcessor
import org.jetbrains.plugins.scala.lang.resolve.{ResolveTargets, ScalaResolveState}
import org.jetbrains.plugins.scala.project.ProjectContext

import scala.collection.mutable

/**
  * Created by Kate Ustyuzhanina
  * on 8/3/15
  */
class ScalaTypeValidator(val selectedElement: PsiElement, override val noOccurrences: Boolean, enclosingContainerAll: PsiElement, enclosingOne: PsiElement)
  extends ScalaValidator(selectedElement, noOccurrences, enclosingContainerAll, enclosingOne) {

  private implicit def ctx: ProjectContext = selectedElement

  protected override def findConflictsImpl(name: String, allOcc: Boolean): Seq[(PsiNamedElement, String)] = {
    //returns declaration and message
    val container = enclosingContainer(allOcc)
    if (container == null) return Seq.empty

    forbiddenNames(container, name) match {
      case Seq() => forbiddenNamesInBlock(container, name)
      case seq => seq
    }
  }

  import ScalaTypeValidator.*

  protected def forbiddenNames(position: PsiElement, name: String): Seq[(PsiNamedElement, String)] = {
    val builder = Seq.newBuilder[(PsiNamedElement, String)]

    val processor = new BaseProcessor(ValueSet(ResolveTargets.CLASS)) {

      override protected def execute(namedElement: PsiNamedElement)
                                    (implicit state: ResolveState): Boolean = {
        for {
          msg <- message(namedElement, name)
        } builder += namedElement -> msg

        true
      }
    }
    PsiTreeUtil.treeWalkUp(processor, position, null, ScalaResolveState.empty)

    builder.result()
  }

  protected def forbiddenNamesInBlock(commonParent: PsiElement, name: String): Seq[(PsiNamedElement, String)] = {
    val builder = Seq.newBuilder[(PsiNamedElement, String)]

    for {
      namedElement <- commonParent.depthFirst().collect {
        case named: PsiNamedElement => named
      }
      msg <- message(namedElement, name)
    } builder += namedElement -> msg

    builder.result()
  }

  override def validateName(name: String): String =
    super.validateName(name.capitalize)
}

object ScalaTypeValidator {

  def empty: ScalaTypeValidator =
    new ScalaTypeValidator(null, noOccurrences = true, null, null) {
      override def validateName(name: String): String = name
    }

  def apply(element: PsiElement, container: PsiElement, noOccurrences: Boolean) =
    new ScalaTypeValidator(element, noOccurrences, container, container)

  private def message(namedElement: PsiNamedElement,
                      name: String): Option[String] =
    (namedElement.name == name)
      .option(namedElement)
      .collect {
        case _: ScTypeAlias | _: ScTypeParam =>
          ScalaBundle.message("introduced.typeAlias.will.conflict.with.type.name", name)
        case typeDefinition: ScTypeDefinition if getParentOfType(typeDefinition, classOf[ScFunctionDefinition]) == null =>
          ScalaBundle.message("introduced.typeAlias.will.conflict.with.class.name", name)
      }
}
