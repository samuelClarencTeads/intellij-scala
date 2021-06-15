package org.jetbrains.plugins.scala
package codeInspection
package packageNameInspection

import com.intellij.codeInspection.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.{ProjectFileIndex, ProjectRootManager}
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.lang.refactoring.ScalaNamesValidator.isKeyword
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil.isBacktickedName
import org.jetbrains.plugins.scala.util.IntentionAvailabilityChecker

/**
 * User: Alexander Podkhalyuzin
 * Date: 08.07.2009
 */

class ScalaPackageNameInspection extends LocalInspectionTool {
  override def isEnabledByDefault: Boolean = true

  override def getID: String = "ScalaPackageName"

  override def checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array[ProblemDescriptor] = {
    file match {
      case file: ScalaFile if IntentionAvailabilityChecker.checkInspection(this, file) =>
        if (file.isScriptFile) return null
        if (file.isWorksheetFile) return null
        if (file.typeDefinitions.isEmpty) return null

        val dir = file.getContainingDirectory
        if (dir == null) return null
        val pack = JavaDirectoryService.getInstance.getPackage(dir)
        if (pack == null) return null

        val packName = cleanKeywords(file.getPackageName)
        val ranges: Seq[TextRange] = file.packagingRanges match {
          case Seq() => file.typeDefinitions.map(_.nameId.getTextRange)
          case seq => seq
        }

        def problemDescriptors(buffer: Seq[LocalQuickFix]): Seq[ProblemDescriptor] = ranges.map { range =>
          var message = ScalaInspectionBundle.message("package.names.does.not.correspond.to.directory.structure", packName, pack.getQualifiedName)

          // Specifically make sure that the file path doesn't repeat an existing package prefix (twice).
          for (virtualFile <- Option(file.getVirtualFile);
               sourceFolder <- Option(ProjectRootManager.getInstance(file.getProject).getFileIndex.getSourceFolder(virtualFile));
               packagePrefix = sourceFolder.getPackagePrefix if !packagePrefix.isEmpty
               if (pack.getQualifiedName + ".").startsWith(packagePrefix + "." + packagePrefix + ".")) {
            message += "\n\n" + ScalaInspectionBundle.message("package.names.does.not.correspond.to.directory.structure.package.prefix", sourceFolder.getFile.getName, packagePrefix)
          }

          manager.createProblemDescriptor(file, range, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly, buffer*)
        }

        val expectedPackageName = file.typeDefinitions.head match {
          case obj: ScObject if obj.hasPackageKeyword =>
            Option(pack.getParentPackage).map(_.getQualifiedName).getOrElse("")
          case _ =>
            pack.getQualifiedName
        }

        if (packName == null) {
          val fixes = Seq.empty[LocalQuickFix]
          problemDescriptors(fixes).toArray
        } else if (packName != expectedPackageName) {
          val fixes = Seq(
            new ScalaRenamePackageQuickFix(file, expectedPackageName),
            new ScalaMoveToPackageQuickFix(file, packName))

          problemDescriptors(fixes).toArray
        } else null
      case _ => null
    }
  }

  private def cleanKeywords(packageName: String): String = {
    if (packageName == null) return null
    packageName.split('.').map {
      case isBacktickedName(name) if isKeyword(name) => name
      case name => name
    }.mkString(".")
  }
}