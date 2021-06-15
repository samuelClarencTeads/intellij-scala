package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl


import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubElement}
import com.intellij.util.SofterReference
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParam
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.*

/**
  * User: Alexander Podkhalyuzin
  * Date: 17.06.2009
  */
class ScTypeParamStubImpl(parent: StubElement[? <: PsiElement],
                          elementType: IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement],
                          name: String,
                          override val text: String,
                          override val lowerBoundText: Option[String],
                          override val upperBoundText: Option[String],
                          override val viewBoundsTexts: Array[String],
                          override val contextBoundsTexts: Array[String],
                          override val isCovariant: Boolean,
                          override val isContravariant: Boolean,
                          override val containingFileName: String)
  extends ScNamedStubBase[ScTypeParam](parent, elementType, name)
    with ScTypeParamStub with ScBoundsOwnerStub[ScTypeParam] {

  private var viewElementsReferences: SofterReference[Seq[ScTypeElement]] = null
  private var contextElementsReferences: SofterReference[Seq[ScTypeElement]] = null

  override def viewBoundsTypeElements: Seq[ScTypeElement] = {
    getFromReference(viewElementsReferences) {
      case (context, child) =>
        viewBoundsTexts.map {
          createTypeElementFromText(_, context, child)
        }.toSeq
    } (viewElementsReferences = _)
  }

  override def contextBoundsTypeElements: Seq[ScTypeElement] = {
    getFromReference(contextElementsReferences) {
      case (context, child) =>
        contextBoundsTexts.map {
          createTypeElementFromText(_, context, child)
        }.toSeq
    } (contextElementsReferences = _)
  }
}