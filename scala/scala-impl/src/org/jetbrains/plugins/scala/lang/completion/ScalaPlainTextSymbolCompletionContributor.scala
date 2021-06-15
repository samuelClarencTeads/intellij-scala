package org.jetbrains.plugins.scala
package lang
package completion

import java.{util as ju}

import com.intellij.codeInsight.completion.PlainTextSymbolCompletionContributor
import com.intellij.codeInsight.lookup.{LookupElement, LookupElementBuilder}
import com.intellij.psi.PsiFile
import javax.swing.Icon
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScDeclaredElementsHolder
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScMember, ScTypeDefinition}

final class ScalaPlainTextSymbolCompletionContributor extends PlainTextSymbolCompletionContributor {

  import ScalaPlainTextSymbolCompletionContributor.*

  override def getLookupElements(file: PsiFile,
                                 invocationCount: Int,
                                 prefix: String): ju.Collection[LookupElement] =
    file match {
      case scalaFile: ScalaFile =>
        val result = new ju.ArrayList[LookupElement]
        for {
          definition <- scalaFile.typeDefinitions

          name = definition.name
          if name != null
        } {
          result.add(createElement(name, definition.getIcon(0)))

          val topLevelOnly = invocationCount == 0
          splitAtDot(prefix, name) match {
            case Some(suffix) =>
              processClassBody(result, definition, topLevelOnly)(name, suffix)
            case _ if !topLevelOnly =>
              foreachMemberIn(definition) {
                case (_, memberName, icon) => result.add(createElement(memberName, icon))
              }
            case _ =>
          }
        }

        result
      case _ => ju.Collections.emptyList()
    }
}

object ScalaPlainTextSymbolCompletionContributor {

  private def foreachMemberIn(definition: ScTypeDefinition)
                             (action: (ScMember, String, Icon) => Unit): Unit = for {
    member <- definition.members
    if member.isPhysical

    memberName <- member match {
      case holder: ScDeclaredElementsHolder => holder.declaredNames
      case named: ScNamedElement => Option(named.name).toSeq
      case _ => Seq.empty
    }
  } action(member, memberName, member.getIcon(0))

  private def processClassBody(result: ju.List[LookupElement],
                               definition: ScTypeDefinition,
                               topLevelOnly: Boolean)
                              (prefix: String,
                               suffix: String): Unit =
    foreachMemberIn(definition) {
      case (member, memberName, icon) =>
        if (!topLevelOnly) {
          result.add(createElement(memberName, icon))
        }

        val newPrefix = prefix + "." + memberName
        result.add(createElement(newPrefix, icon))

        member match {
          case innerDefinition: ScTypeDefinition =>
            splitAtDot(suffix, memberName) match {
              case Some(newSuffix) =>
                processClassBody(result, innerDefinition, topLevelOnly = true)(newPrefix, newSuffix)
              case _ =>
            }
          case _ =>
        }
    }

  private def splitAtDot(currentPrefix: String, name: String): Option[String] = {
    val nameWithDot = name + "."
    if (currentPrefix.startsWith(nameWithDot))
      Some(currentPrefix.substring(nameWithDot.length))
    else
      None
  }

  private def createElement(name: String, icon: Icon) =
    LookupElementBuilder.create(name).withIcon(icon)
}
