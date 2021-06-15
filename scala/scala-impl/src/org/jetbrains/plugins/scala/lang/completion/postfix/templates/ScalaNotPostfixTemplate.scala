package org.jetbrains.plugins.scala.lang
package completion
package postfix
package templates

import com.intellij.codeInsight.template.postfix.templates.NotPostfixTemplate
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector.*
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.ScalaPostfixTemplatePsiInfo

/**
 * @author Roman.Shein
 * @since 11.09.2015.
 */
final class ScalaNotPostfixTemplate(alias: String = "not") extends NotPostfixTemplate(
  alias,
  "." + alias,
  "!expr",
  ScalaPostfixTemplatePsiInfo,
  SelectAllAncestors(BooleanExpression)
)