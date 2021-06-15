package org.jetbrains.plugins.scala.lang.structureView

import java.util
import java.util.Collections

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.ide.util.FileStructureNodeProvider
import com.intellij.ide.util.treeView.smartTree.{ActionPresentation, ActionPresentationData, TreeElement}
import com.intellij.openapi.actionSystem.Shortcut
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.{PsiElement, PsiMethod}
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScTypeAlias, ScValue, ScVariable}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.types.PhysicalMethodSignature
import org.jetbrains.plugins.scala.lang.structureView.element.Element

import scala.jdk.CollectionConverters.*

/**
 * @author Alefas
 * @since 04.05.12
 */

class ScalaInheritedMembersNodeProvider extends FileStructureNodeProvider[TreeElement] {
  override def provideNodes(node: TreeElement): util.Collection[TreeElement] = node match {
    case e: Element => nodesOf(e.element)
    case _ => Collections.emptyList[TreeElement]
  }

  private def nodesOf(element: PsiElement): util.Collection[TreeElement] = {
    element match {
      case clazz: ScTypeDefinition =>
        val children = new util.ArrayList[TreeElement]()
        try {
          if (!clazz.isValid) return children
          for (sign <- clazz.allSignatures) {
            sign match {
              case sign: PhysicalMethodSignature =>
                sign.method match {
                  case x if x.name == "$tag" || x.name == "$init$" =>
                  case x if x.containingClass == clazz =>
                  case x: ScFunction => children.addAll(Element(x, inherited = true).asJava)
                  case x: PsiMethod => children.add(new PsiMethodTreeElementDecorator(x, true))
                }
              case _ =>
                sign.namedElement match {
                  case parameter: ScClassParameter if parameter.isClassMember && parameter.containingClass != clazz && !sign.name.endsWith("_=") =>
                    children.addAll(Element(parameter, inherited = true).asJava)
                  case named: ScNamedElement => ScalaPsiUtil.nameContext(named) match {
                    case x: ScValue if x.containingClass != clazz => children.addAll(Element(named, inherited = true).asJava)
                    case x: ScVariable if x.containingClass != clazz => children.addAll(Element(named, inherited = true).asJava)
                    case _ =>
                  }
                  case _ =>
                }
            }
          }
          clazz.allTypeSignatures.map(_.namedElement).collect {
            case alias: ScTypeAlias if alias.containingClass != clazz =>
              children.addAll(Element(alias, inherited = true).asJava)
          }

          children
        }
        catch {
          case _: IndexNotReadyException => Collections.emptyList[TreeElement]
        }
      case _ => Collections.emptyList[TreeElement]
    }
  }

  override def getCheckBoxText: String = IdeBundle.message("file.structure.toggle.show.inherited")

  override def getShortcut: Array[Shortcut] = KeymapManager.getInstance.getActiveKeymap.getShortcuts("FileStructurePopup")

  override def getPresentation: ActionPresentation = new ActionPresentationData(
    IdeBundle.message("action.structureview.show.inherited"), null, AllIcons.Hierarchy.Supertypes)

  override def getName: String = "SCALA_SHOW_INHERITED"
}
