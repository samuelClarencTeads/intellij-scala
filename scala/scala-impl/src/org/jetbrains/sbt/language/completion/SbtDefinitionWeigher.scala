package org.jetbrains.sbt
package language
package completion

import com.intellij.codeInsight.completion.{CompletionLocation, CompletionWeigher}
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.plugins.scala.lang.completion.lookups.ScalaLookupItem

/**
 * @author Nikolay Obedin
 * @since 7/10/14
 */
final class SbtDefinitionWeigher extends CompletionWeigher {

  override def weigh(element: LookupElement,
                     location: CompletionLocation): Comparable[?] = element match {
    case element: ScalaLookupItem
      if element.isSbtLookupItem &&
        element.getLookupString != "???" =>
      if (element.isLocalVariable) 2 else 1
    case _ => 0
  }
}
