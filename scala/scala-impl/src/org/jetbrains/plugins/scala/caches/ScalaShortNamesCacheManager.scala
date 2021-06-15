package org.jetbrains.plugins.scala
package caches

import com.intellij.openapi.project.{DumbService, Project}
import com.intellij.psi.*
import com.intellij.psi.search.{GlobalSearchScope, PsiShortNamesCache}
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScValueOrVariable}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScObject, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.light.PsiMethodWrapper
import org.jetbrains.plugins.scala.lang.psi.stubs.index.ScalaIndexKeys.*
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil.*

import scala.util.Try
import scala.collection.immutable.ArraySeq.unsafeWrapArray

final class ScalaShortNamesCacheManager(implicit project: Project) {

  def getClassByFQName(fqn: String, scope: GlobalSearchScope): PsiClass = {
    if (DumbService.getInstance(project).isDumb) {
      return null
    }

    classesFromIndex(fqn, scope)
      .find {
        case cls if cls.qualifiedName != null && equivalentFqn(fqn, cls.qualifiedName) =>
          // throws PsiInvalidElementAccessException
          Try(cls.getContainingFile)
            .map {
              case file: ScalaFile if file.isScriptFile =>
                false
              case _ =>
                true
            }
            .getOrElse(false)

        case _ => false
      }
      .orNull
  }

  def getClassesByFQName(fqn: String, scope: GlobalSearchScope): Seq[PsiClass] = {
    if (DumbService.getInstance(project).isDumb) {
      return Nil
    }
    classesFromIndex(fqn, scope)
      .filter(cls => cls.qualifiedName != null && equivalentFqn(fqn, cls.qualifiedName))
      .flatMap {
        case cls: ScTypeDefinition => // Add fakeCompanionModule when ScTypeDefinition
          Seq(cls) ++ cls.fakeCompanionModule.toSeq
        case cls =>
          Seq(cls)
      }
      .toIndexedSeq
  }

  def methodsByName(name: String)
                   (implicit scope: GlobalSearchScope): Iterable[PsiMethod] = {
    val cleanName = cleanFqn(name)
    allFunctionsByName(cleanName) ++ allMethodsByName(cleanName, psiNamesCache)
  }

  def getClassesByName(name: String, scope: GlobalSearchScope): Iterable[PsiClass] =
    SHORT_NAME_KEY.elements(name, scope)

  def findPackageObjectByName(fqn: String, scope: GlobalSearchScope): Option[ScObject] = {
    if (DumbService.getInstance(project).isDumb) {
      None
    } else {
      classesFromIndex(fqn, scope, indexKey = PACKAGE_OBJECT_KEY)
        .collectFirst {
          case scalaObject: ScObject if scalaObject.qualifiedName != null &&
            equivalentFqn(fqn, scalaObject.qualifiedName.stripSuffix(".`package`")) => scalaObject
        }
    }
  }

  def allProperties(predicate: String => Boolean)
                   (implicit scope: GlobalSearchScope): Iterable[ScValueOrVariable] =
    for {
      propertyName <- PROPERTY_NAME_KEY.allKeys ++ CLASS_PARAMETER_NAME_KEY.allKeys
      if predicate(propertyName)

      cleanName = cleanFqn(propertyName)

      property <- PROPERTY_NAME_KEY.elements(cleanName, scope)
      if property.declaredNames.map(cleanFqn).contains(cleanName)
    } yield property

  def allFunctions(predicate: String => Boolean)
                  (implicit scope: GlobalSearchScope): Iterable[ScFunction] =
    for {
      functionName <- METHOD_NAME_KEY.allKeys
      if predicate(functionName)

      function <- allFunctionsByName(cleanFqn(functionName))
    } yield function

  private def allFunctionsByName(cleanName: String)
                                (implicit scope: GlobalSearchScope): Iterable[ScFunction] =
    METHOD_NAME_KEY.elements(cleanName, scope).filter { function =>
      equivalentFqn(cleanName, function.name)
    }

  def allFields(predicate: String => Boolean)
               (implicit scope: GlobalSearchScope): Iterable[PsiField] = {
    val namesCache = psiNamesCache

    for {
      fieldName <- unsafeWrapArray(namesCache.getAllFieldNames)
      if predicate(fieldName)

      field <- namesCache.getFieldsByName(fieldName, scope)
    } yield field
  }

  def allMethods(predicate: String => Boolean)
                (implicit scope: GlobalSearchScope): Iterable[PsiMethod] = {
    val namesCache = psiNamesCache

    for {
      methodName <- unsafeWrapArray(namesCache.getAllMethodNames)
      if predicate(methodName)

      method <- allMethodsByName(cleanFqn(methodName), namesCache)
    } yield method
  }

  private def allMethodsByName(cleanName: String, namesCache: PsiShortNamesCache)
                              (implicit scope: GlobalSearchScope) =
    namesCache.getMethodsByName(cleanName, scope).filter {
      case _: ScFunction |
           _: PsiMethodWrapper[?] => false
      case _ => true
    }

  private def psiNamesCache = PsiShortNamesCache.getInstance(project)

  private def classesFromIndex(name: String, scope: GlobalSearchScope,
                              indexKey: StubIndexKey[java.lang.Integer, PsiClass] = FQN_KEY): Iterable[PsiClass] =
    indexKey.elementsByHash(name, scope)
}

object ScalaShortNamesCacheManager {

  def getInstance(implicit project: Project): ScalaShortNamesCacheManager =
    project.getService(classOf[ScalaShortNamesCacheManager])
}
