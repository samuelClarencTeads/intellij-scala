package org.jetbrains.plugins.scala
package overrideImplement

import com.intellij.codeInsight.generation.{GenerateMembersUtil, ClassMember as JClassMember}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.base.Constructor
import org.jetbrains.plugins.scala.lang.psi.api.statements.*
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScTemplateBody
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScMember, ScTemplateDefinition, ScTrait, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createOverrideImplementVariableWithClass
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.TypeDefinitionMembers.*
import org.jetbrains.plugins.scala.lang.psi.types.*
import org.jetbrains.plugins.scala.lang.resolve.ResolveUtils
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.plugins.scala.statistics.{FeatureKey, Stats}

import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.*

/**
 * User: Alexander Podkhalyuzin
 * Date: 08.07.2008
 */

object ScalaOIUtil {

  private[this] def toClassMember(signature: Signature, isOverride: Boolean): Option[ClassMember] = {
    val Signature(named, substitutor) = signature
    val maybeContext = Option(named.nameContext)

    def createMember(parameter: ScClassParameter): ScValue = {
      implicit val projectContext: ProjectContext = parameter.projectContext

      createOverrideImplementVariableWithClass(
        variable = parameter,
        substitutor = substitutor,
        needsOverrideModifier = true,
        isVal = true,
        clazz = parameter.containingClass
      ).asInstanceOf[ScValue]
    }

    named match {
      case typedDefinition: ScTypedDefinition =>
        maybeContext.collect {
          case x: ScValue => new ScValueMember(x, typedDefinition, substitutor, isOverride)
          case x: ScVariable => new ScVariableMember(x, typedDefinition, substitutor, isOverride)
          case x: ScClassParameter if x.isVal => new ScValueMember(createMember(x), typedDefinition, substitutor, isOverride)
        }
      case _ =>
        maybeContext.collect {
          case x: ScTypeAlias if x.containingClass != null => ScAliasMember(x, substitutor, isOverride)
          case x: PsiField => JavaFieldMember(x, substitutor)
        }
    }
  }

  def invokeOverrideImplement(file: PsiFile, isImplement: Boolean,
                              methodName: String = null)
                             (implicit project: Project, editor: Editor): Unit = {

    Stats.trigger(FeatureKey.overrideImplement)

    val clazz = file.findElementAt(editor.getCaretModel.getOffset - 1)
      .parentOfType(classOf[ScTemplateDefinition], strict = false)
      .getOrElse(return)

    val classMembers =
      if (isImplement) getMembersToImplement(clazz, withSelfType = true)
      else getMembersToOverride(clazz)
    if (classMembers.isEmpty) return

    val selectedMembers =
      if (!ApplicationManager.getApplication.isUnitTestMode) {
        val chooser = new ScalaMemberChooser[ClassMember](classMembers.to(ArraySeq), false, true, isImplement, true, true, clazz)
        chooser.setTitle(if (isImplement) ScalaBundle.message("select.method.implement") else ScalaBundle.message("select.method.override"))
        if (isImplement) chooser.selectElements(classMembers.toArray[JClassMember])
        chooser.show()

        val elements = chooser.getSelectedElements
        if (elements == null || elements.isEmpty) {
          return
        }
        elements.asScala.to(ArraySeq)
      } else {
        classMembers.find {
          case named: ScalaNamedMember if named.name == methodName => true
          case _ => false
        }.to(ArraySeq)
      }

    runAction(selectedMembers, isImplement, clazz)
  }


  def runAction(selectedMembers: Seq[ClassMember],
                isImplement: Boolean,
                clazz: ScTemplateDefinition)
               (implicit project: Project, editor: Editor): Unit =
    executeWriteActionCommand(if (isImplement) ScalaBundle.message("action.implement.method") else ScalaBundle.message("action.override.method")) {
      val sortedMembers = ScalaMemberChooser.sorted(selectedMembers, clazz)
      val genInfos = sortedMembers.map(new ScalaGenerationInfo(_))
      val anchor = getAnchor(editor.getCaretModel.getOffset, clazz)

      val inserted = GenerateMembersUtil.insertMembersBeforeAnchor(clazz, anchor.orNull, genInfos.reverse.asJava).asScala
      inserted.lastOption.foreach(_.positionCaret(editor, toEditMethodBody = true))
    }

  def getMembersToImplement(clazz: ScTemplateDefinition, withOwn: Boolean = false, withSelfType: Boolean = false): Seq[ClassMember] =
    classMembersWithFilter(clazz, withSelfType, isOverride = false)(needImplement(_, clazz, withOwn), needImplement(_, clazz, withOwn))

  def getAllMembersToOverride(clazz: ScTemplateDefinition): Seq[ClassMember] =
    classMembersWithFilter(clazz, withSelfType = true)(Function.const(true), Function.const(true))

  def getMembersToOverride(clazz: ScTemplateDefinition): Seq[ClassMember] =
    classMembersWithFilter(clazz, withSelfType = true)(needOverride(_, clazz), needOverride(_, clazz))

  private[this] def classMembersWithFilter(definition: ScTemplateDefinition,
                                           withSelfType: Boolean,
                                           isOverride: Boolean = true)
                                          (f1: PhysicalMethodSignature => Boolean,
                                           f2: PsiNamedElement => Boolean): Seq[ClassMember] = {
    val maybeThisType = if (withSelfType)
      for {
        selfType <- definition.selfType
        clazzType = definition.getTypeWithProjections().getOrAny

        glb = selfType.glb(clazzType)
        if glb.isInstanceOf[ScCompoundType]
      } yield (glb.asInstanceOf[ScCompoundType], Some(clazzType))
    else
      None

    val types = maybeThisType.fold(getTypes(definition)) {
      case (compoundType, compoundTypeThisType) => getTypes(compoundType, compoundTypeThisType)
    }.allSignatures

    val signatures = maybeThisType.fold(getSignatures(definition)) {
      case (compoundType, compoundTypeThisType) => getSignatures(compoundType, compoundTypeThisType)
    }

    val methods = signatures.allSignatures.filter {
      case signature: PhysicalMethodSignature if signature.namedElement.isValid => f1(signature)
      case _ => false
    }.map {
      case signature: PhysicalMethodSignature =>
        val method = signature.method
        assert(method.containingClass != null, s"Containing Class is null: ${method.getText}")
        ScMethodMember(signature, isOverride)
    }

    val values = signatures.allSignatures.filter(isValSignature)

    val aliasesAndValues = (types ++ values).filter {
      case Signature(named, _) => named.isValid && f2(named)
    }.flatMap {
      toClassMember(_, isOverride)
    }

    (methods ++ aliasesAndValues).toSeq
  }

  def isProductAbstractMethod(m: PsiMethod, clazz: PsiClass,
                              visited: Set[PsiClass] = Set.empty): Boolean = {
    if (visited.contains(clazz)) return false
    clazz match {
      case td: ScTypeDefinition if td.isCase =>
        if (m.name == "apply") return true
        if (m.name == "canEqual") return true
        val clazz = m.containingClass
        clazz != null && clazz.qualifiedName == "scala.Product" &&
          (m.name match {
            case "productArity" | "productElement" => true
            case _ => false
          })
      case x: ScTemplateDefinition =>
        x.superTypes.map(_.extractClass).find {
          case Some(c) => isProductAbstractMethod(m, c, visited + clazz)
          case _ => false
        } match {
          case Some(_) => true
          case _ => false
        }
      case _ => false
    }
  }

  private def needOverride(sign: PhysicalMethodSignature, clazz: ScTemplateDefinition): Boolean = {
    sign.method match {
      case method if isProductAbstractMethod(method, clazz) => true
      case f: ScFunctionDeclaration if !f.isNative => false
      case x if x.name == "$tag" || x.name == "$init$" => false
      case x: ScFunction if x.isCopyMethod && x.isSynthetic => false
      case x if x.containingClass == clazz => false
      case x: PsiModifierListOwner if (x.hasModifierPropertyScala("abstract") &&
        !x.isInstanceOf[ScFunctionDefinition])
        || x.hasModifierPropertyScala("final") => false
      case Constructor(_) => false
      case method if !ResolveUtils.isAccessible(method, clazz.extendsBlock) => false
      case method =>
        var flag = false
        if (method match {
          case x: ScFunction => x.parameters.isEmpty
          case _ => method.getParameterList.getParametersCount == 0
        }) {
          for (term <- clazz.allVals; v = term.namedElement) if (v.name == method.name) {
            v.nameContext match {
              case x: ScValue if x.containingClass == clazz => flag = true
              case x: ScVariable if x.containingClass == clazz => flag = true
              case _ =>
            }
          }
        }
        !flag
    }
  }

  private def needImplement(sign: PhysicalMethodSignature, clazz: ScTemplateDefinition, withOwn: Boolean): Boolean = {
    val m = sign.method
    val name = if (m == null) "" else m.name
    val place = clazz.extendsBlock
    m match {
      case _ if isProductAbstractMethod(m, clazz) => false
      case method if !ResolveUtils.isAccessible(method, place) => false
      case _ if name == "$tag" || name == "$init$" => false
      case x if !withOwn && x.containingClass == clazz => false
      case x if x.containingClass != null && x.containingClass.isInterface &&
        !x.containingClass.isInstanceOf[ScTrait] && x.hasModifierProperty("abstract") => true
      case x if x.hasModifierPropertyScala("abstract") && !x.isInstanceOf[ScFunctionDefinition] &&
        !x.isInstanceOf[ScPatternDefinition] && !x.isInstanceOf[ScVariableDefinition] => true
      case x: ScFunctionDeclaration if !x.isNative => true
      case _ => false
    }
  }

  private def needOverride(named: PsiNamedElement, clazz: ScTemplateDefinition) = {
    named.nameContext match {
      case x: PsiModifierListOwner if x.hasModifierPropertyScala("final") => false
      case m: PsiMember if !ResolveUtils.isAccessible(m, clazz.extendsBlock) => false
      case x: ScValue if x.containingClass != clazz =>
        var flag = false
        for (signe <- clazz.allMethods if signe.method.containingClass == clazz) {
          //containingClass == clazz so we sure that this is ScFunction (it is safe cast)
          signe.method match {
            case fun: ScFunction if fun.parameters.isEmpty && x.declaredElements.exists(_.name == fun.name) =>
              flag = true
            case _ => //todo: ScPrimaryConstructor?
          }
        }
        for (term <- clazz.allVals; v = term.namedElement) if (v.name == named.name) {
          v.nameContext match {
            case x: ScValue if x.containingClass == clazz => flag = true
            case _ =>
          }
        }
        !flag
      case x: ScTypeAliasDefinition => x.containingClass != clazz
      case x: ScClassParameter if x.isVal => x.containingClass != clazz
      case _ => false
    }
  }

  private def needImplement(named: PsiNamedElement, clazz: ScTemplateDefinition, withOwn: Boolean): Boolean = {
    named.nameContext match {
      case m: PsiMember if !ResolveUtils.isAccessible(m, clazz.extendsBlock) => false
      case x: ScValueDeclaration if withOwn || x.containingClass != clazz => true
      case x: ScVariableDeclaration if withOwn || x.containingClass != clazz => true
      case x: ScTypeAliasDeclaration if withOwn || x.containingClass != clazz => true
      case _ => false
    }
  }

  def getAnchor(offset: Int, clazz: ScTemplateDefinition): Option[ScMember] = {
    val body: ScTemplateBody = clazz.extendsBlock.templateBody match {
      case Some(x) => x
      case None => return None
    }
    var element: PsiElement = body.getContainingFile.findElementAt(offset)
    while (element != null && element.getParent != body) element = element.getParent

    element match {
      case member: ScMember => Some(member)
      case null => None
      case _ => PsiTreeUtil.getNextSiblingOfType(element, classOf[ScMember]) match {
        case null => None
        case member => Some(member)
      }
    }
  }

  def methodSignaturesToOverride(definition: ScTemplateDefinition): Iterator[PhysicalMethodSignature] =
    definition.allSignatures.filter {
      case signature: PhysicalMethodSignature => needOverride(signature, definition)
      case _ => false
    }.map {
      _.asInstanceOf[PhysicalMethodSignature]
    }
}
