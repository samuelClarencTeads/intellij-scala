package org.jetbrains.plugins.scala
package codeInspection.imports

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.codeInspection.AbstractFixOnPsiElement
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.TokenTexts
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportExpr
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createImportExprWithContextFromText

/**
 * @author Ksenia.Sautina
 * @since 4/11/12
 */

class RemoveBracesForSingleImportQuickFix(importExpr: ScImportExpr)
        extends AbstractFixOnPsiElement(ScalaBundle.message("remove.braces.from.import"), importExpr) {

  override protected def doApplyFix(iExpr: ScImportExpr)
                                   (implicit project: Project): Unit = {
    val name =
      if (iExpr.hasWildcardSelector) TokenTexts.importWildcardText(iExpr)
      else iExpr.importedNames.headOption.getOrElse("")
    val text = s"${iExpr.qualifier.get.getText}.$name"

    inWriteAction {
      iExpr.replace(createImportExprWithContextFromText(text, iExpr))
    }
  }
}
