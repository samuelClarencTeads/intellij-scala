package org.jetbrains.plugins.scala
package lang
package parser
package parsing
package base

import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes.*
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder

/**
 * [[LocalModifier]] ::= 'abstract'
 * | 'final'
 * | 'sealed'
 * | 'implicit'
 * | 'lazy'
 * | [[LocalSoftModifier]]
 *
 * @author Alexander Podkhalyuzin
 *         Date: 15.02.2008
 */
object LocalModifier extends ParsingRule {

  override def apply()(implicit builder: ScalaPsiBuilder): Boolean = builder.getTokenType match {
    case `kABSTRACT` |
         `kFINAL` |
         `kSEALED` |
         `kIMPLICIT` |
         `kLAZY` =>
      builder.advanceLexer() // Ate modifier
      true
    case _ => LocalSoftModifier()
  }
}