package org.jetbrains.plugins.scala
package lang
package parser
package parsing
package xml

import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypesEx
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder
import org.jetbrains.plugins.scala.lang.parser.parsing.expressions.*

/**
* @author Alexander Podkhalyuzin
* Date: 18.04.2008
*/

/*
 * ScalaExpr ::= '{' Block '}'
 */

object ScalaExpr {
  def parse(builder: ScalaPsiBuilder): Boolean = {
    builder.getTokenType match {
      case ScalaTokenTypesEx.SCALA_IN_XML_INJECTION_START =>
        builder.advanceLexer()
        builder.enableNewlines()
      case _ => return false
    }
    if (!Block.parse(builder, hasBrace = false, needNode = true)) {
      builder error ErrMsg("xml.scala.expression.exected")
    }
    CommonUtils.eatAllSemicolons(builder)
    builder.getTokenType match {
      case ScalaTokenTypesEx.SCALA_IN_XML_INJECTION_END =>
        builder.advanceLexer()
      case _ => builder error ErrMsg("xml.scala.injection.end.expected")
    }
    builder.restoreNewlinesState()
    true
  }
}