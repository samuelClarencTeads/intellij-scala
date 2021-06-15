package org.jetbrains.plugins.scala
package lang
package psi
package impl
package toplevel
package synthetic

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.IncorrectOperationException
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScObject, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.stubs.index.ScalaIndexKeys
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil
import org.jetbrains.plugins.scala.lang.resolve.ResolveTargets.*
import org.jetbrains.plugins.scala.lang.resolve.processor.BaseProcessor

import scala.collection.mutable

/**
 * @author ilyas
 */
abstract class ScSyntheticPackage(name: String, manager: PsiManager)
  extends LightElement(manager, ScalaLanguage.INSTANCE) with PsiPackage {

  override def handleQualifiedNameChange(newQualifiedName: String): Unit = {
  }
  override def getDirectories: Array[PsiDirectory] = PsiDirectory.EMPTY_ARRAY
  override def checkSetName(s: String): Unit = {
    throw new IncorrectOperationException("cannot set name: nonphysical element")
  }
  override def getText = ""
  override def toString: String = "Scala Synthetic Package " + getQualifiedName
  override def getDirectories(scope: GlobalSearchScope): Array[PsiDirectory] = PsiDirectory.EMPTY_ARRAY
  override def getModifierList: PsiModifierList = ScalaPsiUtil.getEmptyModifierList(getManager)
  override def hasModifierProperty(s: String) = false
  override def getAnnotationList: PsiModifierList = null
  override def getName: String = name
  override def setName(newName: String) = throw new IncorrectOperationException("cannot set name: nonphysical element")
  override def copy = throw new IncorrectOperationException("cannot copy: nonphysical element")
  override def getContainingFile: PsiFile = SyntheticClasses.get(manager.getProject).file
  override def occursInPackagePrefixes: Array[VirtualFile] = VirtualFile.EMPTY_ARRAY

  override def processDeclarations(processor: PsiScopeProcessor,
                                  state: ResolveState,
                                  lastParent: PsiElement,
                                  place: PsiElement): Boolean = {
     processor match {
      case bp: BaseProcessor =>
        if (bp.kinds.contains(PACKAGE)) {
          val subPackages = if (lastParent != null) getSubPackages(lastParent.resolveScope) else getSubPackages
          for (subp <- subPackages) {
            if (!processor.execute(subp, state)) return false
          }
        }
        if (bp.kinds.contains(CLASS) || bp.kinds.contains(OBJECT) || bp.kinds.contains(METHOD)) {
          for (clazz <- getClasses) {
            if (!processor.execute(clazz, state)) return false
          }
        }
        true
      case _ => true
    }
  }
}


object ScSyntheticPackage {

  def apply(fqn: String)
           (implicit project: Project): ScSyntheticPackage = {
    val (name, parentName) = fqn.lastIndexOf(".") match {
      case -1 => (fqn, "")
      case i => (fqn.substring(i + 1), fqn.substring(0, i))
    }

    import ScalaIndexKeys.*
    PACKAGE_FQN_KEY.allElementsByHash(fqn) match {
      case seq if seq.isEmpty =>
        val packages = PACKAGE_OBJECT_KEY.allElementsByHash(fqn)
        if (packages.exists { pc =>
          ScalaNamesUtil.equivalentFqn(pc.qualifiedName, fqn)
        }) {
          new ScSyntheticPackage(name, PsiManager.getInstance(project)) {
            override def getFiles(globalSearchScope: GlobalSearchScope): Array[PsiFile] = Array.empty //todo: ?
            override def containsClassNamed(name: String): Boolean = false

            override def getQualifiedName: String = fqn

            override def getClasses: Array[PsiClass] = Array.empty

            override def getClasses(scope: GlobalSearchScope): Array[PsiClass] = Array.empty

            override def getParentPackage: ScPackageImpl = ScPackageImpl.findPackage(project, parentName)

            override def getSubPackages: Array[PsiPackage] = Array.empty

            override def getSubPackages(scope: GlobalSearchScope): Array[PsiPackage] = Array.empty

            override def findClassByShortName(name: String, scope: GlobalSearchScope): Array[PsiClass] = Array.empty
          }
        } else null
      case packages =>
        val cleanName = ScalaNamesUtil.cleanFqn(fqn)
        packages.filter { pc =>
          ScalaNamesUtil.cleanFqn(pc.fullPackageName).startsWith(cleanName) && cleanName.startsWith(ScalaNamesUtil.cleanFqn(pc.parentPackageName))
        } match {
          case seq if seq.isEmpty => null
          case filtered =>
            new ScSyntheticPackage(name, PsiManager.getInstance(project)) {
              override def getFiles(globalSearchScope: GlobalSearchScope): Array[PsiFile] = Array.empty //todo: ?

              override def findClassByShortName(name: String, scope: GlobalSearchScope): Array[PsiClass] = {
                getClasses.filter(n => ScalaNamesUtil.equivalentFqn(n.name, name))
              }

              override def containsClassNamed(name: String): Boolean = {
                getClasses.exists(n => ScalaNamesUtil.equivalentFqn(n.name, name))
              }

              override def getQualifiedName: String = fqn

              override def getClasses: Array[PsiClass] = {
                filtered.flatMap(p =>
                  if (ScalaNamesUtil.cleanFqn(p.fullPackageName).length == cleanName.length)
                    p.immediateTypeDefinitions.flatMap {
                      case td@(c: ScTypeDefinition) if c.fakeCompanionModule.isDefined =>
                        Seq(td, c.fakeCompanionModule.get)
                      case td => Seq(td)
                    }
                  else Seq.empty).toArray
              }

              override def getClasses(scope: GlobalSearchScope): Array[PsiClass] =
                getClasses.filter { clazz =>
                  val file = clazz.getContainingFile.getVirtualFile
                  file != null && scope.contains(file)
                }

              override def getParentPackage: ScPackageImpl = ScPackageImpl.findPackage(project, parentName)

              override def getSubPackages: Array[PsiPackage] = {
                val buff = new mutable.HashSet[PsiPackage]
                filtered.foreach{
                  p =>
                    def addPackage(tail : String): Unit = {
                      val p = ScPackageImpl.findPackage(project, fqn + "." + tail)
                      if (p != null) buff += p
                    }

                    val fqn1 = p.fullPackageName
                    val tail = if (fqn1.length > fqn.length) fqn1.substring(fqn.length + 1) else ""
                    if (tail.length == 0) {
                      p.packagings.foreach {
                        pack => {
                          val own = pack.packageName
                          val i = own.indexOf(".")
                          addPackage(if (i > 0) own.substring(0, i) else own)
                        }
                      }
                      p.immediateTypeDefinitions.foreach {
                        case o: ScObject if o.isPackageObject && o.getName != "`package`" =>
                          addPackage(o.name)
                        case _ =>
                      }
                    } else {
                      val i = tail.indexOf(".")
                      val next = if (i > 0) tail.substring(0, i) else tail
                      addPackage(next)
                    }
                }
                buff.toArray
              }
              override def getSubPackages(scope: GlobalSearchScope): Array[PsiPackage] = getSubPackages
            }

        }
    }
  }
}
