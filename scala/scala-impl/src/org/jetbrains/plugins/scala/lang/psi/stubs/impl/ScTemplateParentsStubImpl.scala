package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubBase, StubElement}
import com.intellij.util.SofterReference
import org.jetbrains.plugins.scala.lang.psi.api.base.ScConstructorInvocation
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScTemplateParents
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.{createConstructorFromText, createTypeElementFromText}
import org.jetbrains.plugins.scala.lang.psi.stubs.impl.ScTemplateParentsStubImpl.*

/**
  * User: Alexander Podkhalyuzin
  */
final class ScTemplateParentsStubImpl(parent: StubElement[? <: PsiElement],
                                      elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement],
                                      override val parentTypesTexts: Array[String],
                                      override val constructorText: Option[String])
  extends StubBase[ScTemplateParents](parent, elementType) with ScTemplateParentsStub with PsiOwner[ScTemplateParents] {

  private var constructorAndParentTypeElementsReference: SofterReference[Data] = null

  private def constructorAndParentTypeElements: Data = {
    getFromReferenceWithFilter[PsiElement, Data](constructorAndParentTypeElementsReference, {
      case (context, child) =>
        val constructor = constructorText.flatMap { text =>
          Option(createConstructorFromText(text, context, child))
        }
        val parentTypeElems = parentTypesTexts.toSeq.map {
          createTypeElementFromText(_, context, child)
        }
        (constructor, parentTypeElems)
    }, constructorAndParentTypeElementsReference = _)
  }

  override def parentTypeElements: Seq[ScTypeElement] = {
    constructorAndParentTypeElements match {
      case (Some(constr), typeElems) => constr.typeElement +: typeElems
      case (_, typeElems) => typeElems
    }
  }
}

private object ScTemplateParentsStubImpl {
  type Data = (Option[ScConstructorInvocation], Seq[ScTypeElement])

  implicit def flatten: Data => Seq[PsiElement] = {
    case (opt, seq) => opt.toSeq ++ seq
  }
}