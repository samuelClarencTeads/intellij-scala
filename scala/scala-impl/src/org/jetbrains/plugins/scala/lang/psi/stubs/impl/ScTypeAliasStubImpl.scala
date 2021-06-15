package org.jetbrains.plugins.scala
package lang
package psi
package stubs
package impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAlias

/**
  * User: Alexander Podkhalyuzin
  * Date: 18.10.2008
  */
class ScTypeAliasStubImpl(
  parent:                         StubElement[? <: PsiElement],
  elementType:                    IStubElementType[? <: StubElement[? <: PsiElement], ? <: PsiElement],
  name:                           String,
  override val typeText:          Option[String],
  override val lowerBoundText:    Option[String],
  override val upperBoundText:    Option[String],
  override val isLocal:           Boolean,
  override val isDeclaration:     Boolean,
  override val isStableQualifier: Boolean,
  override val stableQualifier:   Option[String],
  override val isTopLevel:        Boolean,
  override val topLevelQualifier: Option[String]
) extends ScNamedStubBase[ScTypeAlias](parent, elementType, name)
    with ScTypeAliasStub
