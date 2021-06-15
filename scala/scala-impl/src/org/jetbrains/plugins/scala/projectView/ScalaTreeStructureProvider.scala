package org.jetbrains.plugins.scala
package projectView

import java.util

import com.intellij.ide.projectView.{TreeStructureProvider, ViewSettings}
import com.intellij.openapi.project.{DumbAware, Project}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition

/**
 * @author Pavel Fatin
 */
final class ScalaTreeStructureProvider extends TreeStructureProvider with DumbAware {

  import ScalaTreeStructureProvider.*

  import scala.jdk.CollectionConverters.*

  override def modify(parent: Node, children: util.Collection[Node], settings: ViewSettings): util.Collection[Node] =
    children.asScala.map { it =>
      transform(it)(it.getProject, settings)
    }.asJavaCollection
}

private object ScalaTreeStructureProvider {

  private def transform(node: Node)
                       (implicit project: Project, settings: ViewSettings): Node = node.getValue match {
    case file: ScalaFile => Node(file)
    case definition: ScTypeDefinition if !node.isInstanceOf[TypeDefinitionNode] =>
      new TypeDefinitionNode(definition)
    case _ => node
  }
}
