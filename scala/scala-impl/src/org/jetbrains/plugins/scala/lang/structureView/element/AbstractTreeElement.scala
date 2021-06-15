package org.jetbrains.plugins.scala.lang.structureView.element

import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.util.HashBuilder.*

// TODO make private (after decoupling Test)
abstract class AbstractTreeElement[T <: PsiElement](override val element: T, override val inherited: Boolean = false)
  extends Element with AbstractNavigatable with AbstractItemPresentation {

  override def getPresentation: ItemPresentation = this

  override def getValue: AnyRef = if (element.isValid) element else null

  override def getChildren: Array[TreeElement] = children.flatMap(Element(_)).toArray

  protected def children: Seq[PsiElement] = Seq.empty

  override def isAlwaysLeaf: Boolean = true

  override def isAlwaysShowsPlus: Boolean = false

  // TODO
  override def equals(o: Any): Boolean = {
    val clazz = o match {
      case obj: Object => obj.getClass
      case _ => return false
    }
    if (o == null || getClass != clazz) return false
    val that = o.asInstanceOf[Element]
    if (inherited != that.inherited) return false

    val value = getValue
    if (value == null) that.getValue == null
    else value == that.getValue
  }

  override def hashCode(): Int = getValue #+ inherited
}

