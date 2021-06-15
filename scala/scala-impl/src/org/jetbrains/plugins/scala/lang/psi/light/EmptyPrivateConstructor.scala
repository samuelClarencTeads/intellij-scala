package org.jetbrains.plugins.scala.lang.psi.light

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightMethodBuilder
import org.jetbrains.plugins.scala.lang.psi.light.EmptyPrivateConstructor.constructorName

/**
 * User: Alefas
 * Date: 20.02.12
 */
class EmptyPrivateConstructor(c: PsiClass) extends LightMethodBuilder(c.getManager, constructorName(c)) {
  addModifier("private")
  setContainingClass(c)

  override def isConstructor: Boolean = true
}

private object EmptyPrivateConstructor {
  private def constructorName(c: PsiClass): String = 
    Option(c.getName)
      .filter(PsiNameHelper.getInstance(c.getProject).isIdentifier)
      .getOrElse("CLASS_NAME_IS_NOT_AN_IDENTIFIER")
}