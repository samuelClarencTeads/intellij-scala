package org.jetbrains.plugins.scala
package conversion
package copy
package plainText

import com.intellij.codeInsight.daemon.JavaErrorBundle
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.impl.source.ScalaCodeFragment

/**
  * Created by Kate Ustyuzhanina on 12/27/16.
  *
  * This code assume that Scala files don't have semicolon at the end of line at all and
  * Java files could miss semicolon at the end of line or last curly bracket.
  * Java file without any semicolon could be treated as scala
  */
object PlainTextCopyUtil {

  private val javaAllowedErrors = Set(
    JavaErrorBundle.message("expected.semicolon"),
    JavaErrorBundle.message("expected.rbrace")
  )

  private val scalaAllowedErrors = Set(
    ScalaBundle.message("rbrace.expected"),
    ScalaBundle.message("semi.expected")
  )

  /**
    * Treat scala file as valid if it doesn't contain ";\n" or one word text or parsed correctly as scala and not parsed correctly as java
    */
  def isValidScalaFile(text: String)
                      (implicit project: Project): Boolean = {
    def withLastSemicolon(text: String): Boolean = (!text.contains("\n") && text.contains(";")) || text.contains(";\n")

    def isOneWord(text: String): Boolean = !text.trim.contains(" ")

    if (withLastSemicolon(text) || isJavaClassWithPublic(text)) false
    else if (isOneWord(text)) true
    else createScalaFile(text).exists(isParsedCorrectly)
  }

  def isJavaClassWithPublic(text: String)
                           (implicit project: Project): Boolean =
    createJavaFile(text).exists(_.getClasses.exists(_.hasModifierProperty("public")))

  def isValidJavaFile(text: String)
                     (implicit project: Project): Boolean = createJavaFile(text).exists(isParsedCorrectly)

  def isParsedCorrectly(file: PsiFile): Boolean = {
    val errorElements = file.depthFirst().filterByType[PsiErrorElement].toList

    if (errorElements.isEmpty) true
    else {
      val allowedMessages = file match {
        case _: ScalaFile => scalaAllowedErrors
        case _            => javaAllowedErrors
      }
      val (allowed, notAllowed) =
        errorElements.partition(err => allowedMessages.contains(err.getErrorDescription))

      notAllowed.isEmpty && allowed.size <= 1
    }
  }

  def createScalaFile(text: String)
                     (implicit project: Project): Some[ScalaFile] = Some(ScalaCodeFragment(text))

  def createJavaFile(text: String)
                    (implicit project: Project): Option[PsiJavaFile] =
    PsiFileFactory.getInstance(project).createFileFromText(
      "Dummy.java",
      JavaFileType.INSTANCE,
      text
    ).asOptionOf[PsiJavaFile]
}
