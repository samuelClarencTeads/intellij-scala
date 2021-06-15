package org.jetbrains.plugins.scala.lang
package completion
package postfix

import java.{util as ju}

import com.intellij.codeInsight.template.postfix.templates.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

import scala.jdk.CollectionConverters.*

/**
  * @author Roman.Shein
  * @since 03.09.2015.
  */
final class ScalaPostfixTemplateProvider extends PostfixTemplateProvider {

  override def getTemplates: ju.Set[PostfixTemplate] = {
    ScalaPostfixTemplateProvider.Templates.asJava
  }

  override def isTerminalSymbol(currentChar: Char): Boolean = currentChar match {
    case '.' | '!' => true
    case _ => false
  }

  override def preExpand(file: PsiFile, editor: Editor): Unit = {}

  override def preCheck(copyFile: PsiFile, realEditor: Editor, currentOffset: Int): PsiFile = copyFile

  override def afterExpand(file: PsiFile, editor: Editor): Unit = {}
}

object ScalaPostfixTemplateProvider {

  import templates.*

  private[postfix] val Templates = Set[PostfixTemplate](
    new ScalaTryPostfixTemplate,
    new ScalaAssertPostfixTemplate,
    new ScalaCastPostfixTemplate,
    new ScalaElseExpressionPostfixTemplate,
    new ScalaIfExpressionPostfixTemplate,
    new ScalaMatchPostfixTemplate,
    new ScalaForEachPostfixTemplate,
    new ScalaIntroduceFieldPostfixTemplate,
    new ScalaIntroduceVariablePostfixTemplate,
    new ScalaNotPostfixTemplate,
    new ScalaNotPostfixTemplate("!"),
    new ScalaParenthesizedExpressionPostfixTemplate,
    new ScalaReturnPostfixTemplate,
    new ScalaPrintlnPostfixTemplate,
    new ScalaPrintlnPostfixTemplate("prtln"),
    new ScalaThrowExceptionPostfixTemplate,
    new ScalaWhilePostfixTemplate,
    new ScalaDoWhilePostfixTemplate,
    new ScalaIsNullPostfixTemplate,
    new ScalaNotNullPostfixTemplate,
    new ScalaNotNullPostfixTemplate("nn"),
    new ScalaOptionPostfixTemplate,
    new ScalaSeqPostfixTemplate,
    new ScalaListPostfixTemplate,
    new ScalaExhaustiveMatchPostfixTemplate
  )
}
