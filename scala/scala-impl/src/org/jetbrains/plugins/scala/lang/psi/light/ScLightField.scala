package org.jetbrains.plugins.scala
package lang.psi.light

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightFieldBuilder
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.types.ScType

/**
 * @author Alefas
 * @since 07.12.12
 */
object ScLightField {

  def apply(name: String, scType: ScType, containingClass: ScTypeDefinition, modifiers: String*): PsiField = {
    new LightFieldBuilder(name, scType.toPsiType, containingClass)
      .setContainingClass(containingClass)
      .setModifiers(modifiers*)
  }
}
