package org.jetbrains.plugins.scala
package lang.psi.implicits

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.macros.evaluator.MacroContext
import org.jetbrains.plugins.scala.lang.macros.evaluator.ScalaMacroEvaluator
import org.jetbrains.plugins.scala.lang.psi.api.InferUtil
import org.jetbrains.plugins.scala.lang.psi.api.InferUtil.SafeCheckException
import org.jetbrains.plugins.scala.lang.psi.api.statements.*
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.lang.psi.implicits.ExtensionConversionHelper.extensionConversionCheck
import org.jetbrains.plugins.scala.lang.psi.implicits.ImplicitCollector.*
import org.jetbrains.plugins.scala.lang.psi.types.*
import org.jetbrains.plugins.scala.lang.psi.types.api.*
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.*
import org.jetbrains.plugins.scala.lang.psi.types.nonvalue.ScTypePolymorphicType
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.AfterUpdate.ProcessSubtypes
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.AfterUpdate.ReplaceWith
import org.jetbrains.plugins.scala.lang.psi.types.result.*
import org.jetbrains.plugins.scala.lang.resolve.*
import org.jetbrains.plugins.scala.lang.resolve.processor.MostSpecificUtil
import org.jetbrains.plugins.scala.macroAnnotations.Measure
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings
import org.jetbrains.plugins.scala.traceLogger.TraceLogger

object ImplicitCollector {

  def cache(project: Project): ImplicitCollectorCache = ScalaPsiManager.instance(project).implicitCollectorCache

  sealed trait ImplicitResult

  sealed trait FullInfoResult extends ImplicitResult

  case object NoResult extends ImplicitResult

  case object OkResult extends FullInfoResult
  case object ImplicitParameterNotFoundResult extends FullInfoResult
  case object DivergedImplicitResult extends FullInfoResult
  case object CantInferTypeParameterResult extends FullInfoResult

  case object TypeDoesntConformResult extends ImplicitResult
  case object BadTypeResult extends ImplicitResult
  case object CantFindExtensionMethodResult extends ImplicitResult
  case object UnhandledResult extends ImplicitResult
  case object FunctionForParameterResult extends ImplicitResult

  case class ImplicitState(place: PsiElement,
                           tp: ScType,
                           expandedTp: ScType,
                           coreElement: Option[ScNamedElement],
                           isImplicitConversion: Boolean,
                           searchImplicitsRecursively: Int,
                           extensionData: Option[ExtensionConversionData],
                           fullInfo: Boolean,
                           previousRecursionState: Option[ImplicitsRecursionGuard.RecursionMap]) {

    def presentableTypeText: String = tp.presentableText(place)
  }

  def probableArgumentsFor(parameter: ScalaResolveResult): Seq[(ScalaResolveResult, FullInfoResult)] = {
    parameter.implicitSearchState.map { state =>
      val collector = new ImplicitCollector(state.copy(fullInfo = true))
      collector.collect().flatMap { r =>
        r.implicitReason match {
          case CantInferTypeParameterResult => Seq.empty
          case reason: FullInfoResult => Seq((r, reason))
          case _ => Seq.empty
        }
      }
    }.getOrElse {
      Seq.empty
    }
  }

  def visibleImplicits(place: PsiElement): Set[ScalaResolveResult] =
    ImplicitSearchScope.forElement(place).cachedVisibleImplicits

  def implicitsFromType(place: PsiElement, scType: ScType): Set[ScalaResolveResult] =
    ImplicitSearchScope.forElement(place).cachedImplicitsByType(scType)

}

/**
 * @param place        The call site
 * @param tp           Search for an implicit definition of this type. May have type variables.
 *
 * User: Alexander Podkhalyuzin
 * Date: 23.11.2009
 */
class ImplicitCollector(place: PsiElement,
                        tp: ScType,
                        expandedTp: ScType,
                        coreElement: Option[ScNamedElement],
                        isImplicitConversion: Boolean,
                        searchImplicitsRecursively: Int = 0,
                        extensionData: Option[ExtensionConversionData] = None,
                        fullInfo: Boolean = false,
                        previousRecursionState: Option[ImplicitsRecursionGuard.RecursionMap] = None) {
  def this(state: ImplicitState) = {
    this(state.place, state.tp, state.expandedTp, state.coreElement, state.isImplicitConversion,
       state.searchImplicitsRecursively, state.extensionData, state.fullInfo, state.previousRecursionState)
  }

  lazy val collectorState: ImplicitState = ImplicitState(place, tp, expandedTp, coreElement, isImplicitConversion,
    searchImplicitsRecursively, extensionData, fullInfo, Some(ImplicitsRecursionGuard.currentMap))

  private val project = place.getProject
  private implicit def ctx: ProjectContext = project

  private val clazz: Option[PsiClass] = tp.extractClass
  private lazy val possibleScalaFunction: Option[Int] = clazz.flatMap(possibleFunctionN)

  private val mostSpecificUtil: MostSpecificUtil = MostSpecificUtil(place, 1)

  private def isExtensionConversion: Boolean = extensionData.isDefined

  def collect(): Seq[ScalaResolveResult] = TraceLogger.func {
    def calc(): Seq[ScalaResolveResult] = {
      clazz match {
        case Some(c) if InferUtil.tagsAndManifists.contains(c.qualifiedName) => return Seq.empty
        case _                                                               =>
      }

      ProgressManager.checkCanceled()

      if (fullInfo) {
        val visible = visibleNamesCandidates
        val fromNameCandidates = collectFullInfo(visible)

        val allCandidates =
          if (fromNameCandidates.exists(_.implicitReason == OkResult)) fromNameCandidates
          else {
            val fromTypeNotVisible = fromTypeCandidates.filterNot(c => visible.exists(_.element == c.element))
            fromNameCandidates ++ collectFullInfo(fromTypeNotVisible)
          }

        //todo: should we also compare types like in MostSpecificUtil.isAsSpecificAs ?
        allCandidates.sortWith(mostSpecificUtil.isInMoreSpecificClass)
      }
      else {
        ImplicitCollector.cache(project)
          .getOrCompute(place, tp, mayCacheResult = !isExtensionConversion) {
            val firstCandidates = compatible(visibleNamesCandidates)
            TraceLogger.log("Compatible candidates from visible Names", firstCandidates)
            if (firstCandidates.exists(_.isApplicable())) firstCandidates
            else {
              val secondCandidates = compatible(fromTypeCandidates)
              TraceLogger.log("No compatible candidates from names, so try from type", secondCandidates)
              if (secondCandidates.nonEmpty) secondCandidates else firstCandidates
            }
          }
      }
    }

    previousRecursionState match {
      case Some(m) =>
        val currentMap = ImplicitsRecursionGuard.currentMap
        try {
          ImplicitsRecursionGuard.setRecursionMap(m)
          calc()
        } finally {
          ImplicitsRecursionGuard.setRecursionMap(currentMap)
        }
      case _ => calc()
    }
  }

  private def visibleNamesCandidates: Set[ScalaResolveResult] =
    ImplicitCollector.visibleImplicits(place)
      .map(_.copy(implicitSearchState = Some(collectorState)))

  private def fromTypeCandidates: Set[ScalaResolveResult] =
    ImplicitCollector.implicitsFromType(place, expandedTp)
      .map(_.copy(implicitSearchState = Some(collectorState)))

  private def compatible(candidates: Set[ScalaResolveResult]): Seq[ScalaResolveResult] = TraceLogger.func {
    //implicits found without local type inference have higher priority
    val withoutLocalTypeInference = collectCompatibleCandidates(candidates, withLocalTypeInference = false)

    val compatible =
      if (withoutLocalTypeInference.nonEmpty) withoutLocalTypeInference
      else collectCompatibleCandidates(candidates, withLocalTypeInference = true)


    mostSpecificUtil.mostSpecificForImplicitParameters(compatible) match {
      case Some(r) => Seq(r)
      case _ => compatible.toSeq
    }
  }

  private def collectFullInfo(candidates: Set[ScalaResolveResult]): Seq[ScalaResolveResult] = TraceLogger.func {
    val allCandidates =
      candidates.flatMap(c => checkCompatible(c, withLocalTypeInference = false)) ++
        candidates.flatMap(c => checkCompatible(c, withLocalTypeInference = true))
    val afterExtensionPredicate = allCandidates.flatMap(applyExtensionPredicate)

    afterExtensionPredicate
      .filter(_.implicitReason.is[FullInfoResult])
      .toSeq
  }

  private def possibleFunctionN(clazz: PsiClass): Option[Int] =
    clazz.qualifiedName match {
      case "java.lang.Object" => Some(-1)
      case name =>
        val paramsNumber = name.stripPrefix(FunctionType.TypeName)
        if (paramsNumber.nonEmpty && paramsNumber.forall(_.isDigit)) Some(paramsNumber.toInt)
        else None
    }

  def checkCompatible(c: ScalaResolveResult, withLocalTypeInference: Boolean, checkFast: Boolean = false): Option[ScalaResolveResult] = TraceLogger.func {
    ProgressManager.checkCanceled()

    c.element match {
      case fun: ScFunction =>
        if (!fun.hasTypeParameters && withLocalTypeInference) return None

        //scala.Predef.$conforms should be excluded
        if (isImplicitConversion && isPredefConforms(fun)) return None

        //to avoid checking implicit functions in case of simple implicit parameter search
        val hasNonImplicitClause = fun.effectiveParameterClauses.exists(!_.isImplicit)
        if (hasNonImplicitClause) {
          val clause = fun.paramClauses.clauses.head
          val paramsCount = clause.parameters.size
          if (!possibleScalaFunction.exists(x => x == -1 || x == paramsCount)) {
            return reportWrong(c, FunctionForParameterResult, Seq(WrongTypeParameterInferred))
          }
        }

        checkFunctionByType(c, withLocalTypeInference, checkFast)

      case _ =>
        if (withLocalTypeInference) None //only functions may have local type inference
        else simpleConformanceCheck(c)
    }
  }

  def collectCompatibleCandidates(candidates: Set[ScalaResolveResult], withLocalTypeInference: Boolean): Set[ScalaResolveResult] = TraceLogger.func {
    var filteredCandidates = Set.empty[ScalaResolveResult]

    val iterator = candidates.iterator
    while (iterator.hasNext) {
      val c = iterator.next()
      filteredCandidates ++= checkCompatible(c, withLocalTypeInference, checkFast = true)
    }

    var results: Set[ScalaResolveResult] = Set()

    while (filteredCandidates.nonEmpty) {
      val next = mostSpecificUtil.nextMostSpecific(filteredCandidates)
      next match {
        case Some(c) =>
          filteredCandidates = filteredCandidates diff Set(c)
          val compatible = checkCompatible(c, withLocalTypeInference)
          val afterExtensionPredicate = compatible.flatMap(applyExtensionPredicate)
          afterExtensionPredicate.foreach { r =>
            if (!r.problems.contains(WrongTypeParameterInferred)) {
              val notMoreSpecific = mostSpecificUtil.notMoreSpecificThan(r)
              filteredCandidates = filteredCandidates.filter(notMoreSpecific)
              //this filter was added to make result deterministic
              results = results.filter(c => notMoreSpecific(c))
              results = results union Set(r)
            }
          }
        case None => filteredCandidates = Set.empty
      }
    }
    results
  }

  private def simpleConformanceCheck(c: ScalaResolveResult): Option[ScalaResolveResult] = TraceLogger.func {
    c.element match {
      case typeable: Typeable =>
        val subst = c.substitutor
        typeable.`type`() match {
          case Right(t) =>
            if (!subst(t).conforms(tp))
              reportWrong(c, TypeDoesntConformResult)
            else
              Some(c.copy(implicitReason = OkResult))
          case _ =>
            reportWrong(c, BadTypeResult)
        }
      case _ => None
    }
  }

  private def inferValueType(tp: ScType): (ScType, Seq[TypeParameter]) = TraceLogger.func {
    if (isExtensionConversion) {
      tp match {
        case ScTypePolymorphicType(internalType, typeParams) =>
          val filteredTypeParams =
            typeParams.filter(tp => !tp.lowerType.equiv(Nothing) || !tp.upperType.equiv(Any))
          val newPolymorphicType = ScTypePolymorphicType(internalType, filteredTypeParams)
          val updated = newPolymorphicType.inferValueType.updateLeaves {
            case u: UndefinedType => u.inferValueType
          }
          (updated, typeParams)
        case _ => (tp.inferValueType, Seq.empty)
      }
    } else tp match {
      case ScTypePolymorphicType(_, typeParams) => (tp.inferValueType, typeParams)
      case _ => (tp.inferValueType, Seq.empty)
    }
  }

  private def updateNonValueType(nonValueType0: ScType): ScType = TraceLogger.func {
    InferUtil.updateAccordingToExpectedType(
      nonValueType0,
      filterTypeParams = isImplicitConversion,
      expectedType = Some(tp),
      place,
      canThrowSCE = true
    )
  }

  private def updateImplicitParameters(
    c:                 ScalaResolveResult,
    nonValueType0:     ScType,
    hasImplicitClause: Boolean,
    hadDependents:     Boolean
  ): Option[ScalaResolveResult] = TraceLogger.func {
    val fun = c.element.asInstanceOf[ScFunction]

    def wrongTypeParam(nonValueType: ScType, result: ImplicitResult): Some[ScalaResolveResult] = {
      val (valueType, typeParams) = inferValueType(nonValueType)
      Some(c.copy(
        problems = Seq(WrongTypeParameterInferred),
        implicitParameterType = Some(valueType),
        implicitReason = result,
        unresolvedTypeParameters = Some(typeParams)
      ))
    }

    def reportParamNotFoundResult(resType: ScType, implicitParams: Seq[ScalaResolveResult]): Option[ScalaResolveResult] = {
      val (valueType, typeParams) = inferValueType(resType)
      reportWrong(
        c.copy(implicitParameters = implicitParams, implicitParameterType = Some(valueType), unresolvedTypeParameters = Some(typeParams)),
        ImplicitParameterNotFoundResult
      )
    }

    def noImplicitParametersResult(nonValueType: ScType): Some[ScalaResolveResult] = {
      val (valueType, typeParams) = inferValueType(nonValueType)
      val result = c.copy(
        implicitParameterType = Some(valueType),
        implicitReason = OkResult,
        unresolvedTypeParameters = Some(typeParams)
      )
      Some(result)
    }

    def fullResult(
      resType:          ScType,
      implicitParams:   Seq[ScalaResolveResult],
      checkConformance: Boolean = false
    ): Option[ScalaResolveResult] = {
      val (valueType, typeParams) = inferValueType(resType)

      val allImportsUsed = implicitParams.map(_.importsUsed).foldLeft(c.importsUsed)(_ ++ _)

      val result = c.copy(
        implicitParameterType = Some(valueType),
        implicitParameters = implicitParams,
        implicitReason = OkResult,
        unresolvedTypeParameters = Some(typeParams),
        importsUsed = allImportsUsed
      )

      if (checkConformance && !valueType.conforms(tp))
        reportWrong(result, TypeDoesntConformResult, Seq(WrongTypeParameterInferred))
      else Some(result)
    }

    def wrongExtensionConversion(nonValueType: ScType): Option[ScalaResolveResult] = {
      extensionData.flatMap { data =>
        inferValueType(nonValueType) match {
          case (FunctionType(rt, _), _) =>
            val newCandidate = c.copy(implicitParameterType = Some(rt))
            if (extensionConversionCheck(data, newCandidate).isEmpty)
              wrongTypeParam(nonValueType, CantFindExtensionMethodResult)
            else None
          //this is not a function, when we still need to pass implicit?..
          case _ =>
            wrongTypeParam(nonValueType, UnhandledResult)
        }
      }
    }

    val nonValueType =
      try {
        val updated = updateNonValueType(nonValueType0)

        if (hadDependents) UndefinedType.revertDependentTypes(updated)
        else updated
      }
      catch {
        case _: SafeCheckException => return wrongTypeParam(nonValueType0, CantInferTypeParameterResult)
      }

    val depth = ScalaProjectSettings.getInstance(project).getImplicitParametersSearchDepth
    val notTooDeepSearch = depth < 0 || searchImplicitsRecursively < depth

    if (hasImplicitClause && notTooDeepSearch) {

      if (!hadDependents) {
        wrongExtensionConversion(nonValueType) match {
          case Some(errorResult) => return Some(errorResult)
          case None              => ()
        }
      }

      try {
        val (resType, implicitParams0) =
          InferUtil.updateTypeWithImplicitParameters(
            nonValueType,
            place,
            Some(fun),
            canThrowSCE = !fullInfo,
            searchImplicitsRecursively + 1,
            fullInfo
          )
        val implicitParams = implicitParams0.getOrElse(Seq.empty)

        if (implicitParams.exists(_.isImplicitParameterProblem))
          reportParamNotFoundResult(resType, implicitParams)
        else
          fullResult(resType, implicitParams, hadDependents)
      }
      catch {
        case _: SafeCheckException => wrongTypeParam(nonValueType, CantInferTypeParameterResult)
      }
    } else {
      noImplicitParametersResult(nonValueType)
    }
  }

  @Measure
  private def checkFunctionType(
    c:                ScalaResolveResult,
    nonValueFunTypes: NonValueFunctionTypes
  ): Option[ScalaResolveResult] = TraceLogger.func {

    def compute(): Option[ScalaResolveResult] = {
      nonValueFunTypes.methodType match {
        case None =>
          Some(c.copy(implicitReason = OkResult))

        case Some(nonValueType0) =>
          try {
            updateImplicitParameters(c, c.substitutor(nonValueType0), nonValueFunTypes.hasImplicitClause, nonValueFunTypes.hadDependents)
          }
          catch {
            case _: SafeCheckException =>
              Some(c.copy(problems = Seq(WrongTypeParameterInferred), implicitReason = UnhandledResult))
          }
      }
    }

    if (isImplicitConversion) compute()
    else {
      val coreTypeForTp = coreType(tp)
      val element = coreElement.getOrElse(place)

      def equivOrDominates(tp: ScType, found: ScType): Boolean =
        found.equiv(tp, ConstraintSystem.empty, falseUndef = false).isRight || dominates(tp, found)

      def checkRecursive(tp: ScType, searches: Seq[ScType]): Boolean = searches.exists(equivOrDominates(tp, _))

      def divergedResult = reportWrong(c, DivergedImplicitResult)

      if (ImplicitsRecursionGuard.isRecursive(element, coreTypeForTp, checkRecursive)) divergedResult
      else {
        ImplicitsRecursionGuard.beforeComputation(element, coreTypeForTp)
        try {
          compute().orElse(divergedResult)
        } finally {
          ImplicitsRecursionGuard.afterComputation(element)
        }
      }
    }
  }

  private def reportWrong(c: ScalaResolveResult, reason: ImplicitResult, problems: Seq[ApplicabilityProblem] = Seq.empty) = {
    if (fullInfo) Some(c.copy(problems = problems, implicitReason = reason))
    else None
  }

  private def isPredefConforms(fun: ScFunction) = {
    val name = fun.name
    val clazz = fun.containingClass
    (name == "conforms" || name == "$conforms") && clazz != null && clazz.qualifiedName == "scala.Predef"
  }

  @Measure
  def checkFunctionByType(
    c:                      ScalaResolveResult,
    withLocalTypeInference: Boolean,
    checkFast:              Boolean
  ): Option[ScalaResolveResult] = TraceLogger.func {
    val fun = c.element.asInstanceOf[ScFunction]

    if (fun.hasTypeParameters && !withLocalTypeInference)
      return None

    val macroEvaluator = ScalaMacroEvaluator.getInstance(project)
    val typeFromMacro = macroEvaluator.checkMacro(fun, MacroContext(place, Some(tp)))

    val nonValueFunctionTypes =
      ImplicitCollector.cache(project).getNonValueTypes(fun, c.substitutor, typeFromMacro)

    nonValueFunctionTypes.undefinedType match {
      case Some(undefined: ScType) =>

        val undefinedConforms =
          isImplicitConversion && checkWeakConformance(undefined, tp) ||
            undefined.conforms(tp)

        if (undefinedConforms) {
          if (checkFast) Some(c)
          else checkFunctionType(c, nonValueFunctionTypes)
        }
        else {
          reportWrong(c, TypeDoesntConformResult)
        }
      case _ =>
        if (!withLocalTypeInference) reportWrong(c, BadTypeResult)
        else None
    }
  }

  private def applyExtensionPredicate(cand: ScalaResolveResult): Option[ScalaResolveResult] = {
    extensionData match {
      case None => Some(cand)
      case Some(data) =>
        extensionConversionCheck(data, cand).orElse {
          reportWrong(cand, CantFindExtensionMethodResult)
        }
    }
  }

  private def abstractsToUpper(tp: ScType): ScType = {
    val noAbstracts = tp.updateLeaves {
      case ScAbstractType(_, _, upper) => upper
    }

    noAbstracts.removeAliasDefinitions()
  }

  private def coreType(tp: ScType): ScType = {
    tp match {
      case ScCompoundType(comps, _, _) => abstractsToUpper(ScCompoundType(comps, Map.empty, Map.empty)).removeUndefines()
      case ScExistentialType(quant, _) => abstractsToUpper(ScExistentialType(quant.recursiveUpdate {
        case arg: ScExistentialArgument => ReplaceWith(arg.upper)
        case _ => ProcessSubtypes
      })).removeUndefines()
      case _ => abstractsToUpper(tp).removeUndefines()
    }
  }

  private def dominates(t: ScType, u: ScType): Boolean = {
    complexity(t) > complexity(u) && topLevelTypeConstructors(t).intersect(topLevelTypeConstructors(u)).nonEmpty
  }

  private def topLevelTypeConstructors(tp: ScType): Set[ScType] = {
    tp match {
      case ScProjectionType(_, element) => Set(ScDesignatorType(element))
      case ParameterizedType(designator, _) => Set(designator)
      case tp@ScDesignatorType(_: ScObject) => Set(tp)
      case ScDesignatorType(v: ScTypedDefinition) =>
        val valueType: ScType = v.`type`().getOrAny
        topLevelTypeConstructors(valueType)
      case ScCompoundType(comps, _, _) => comps.flatMap(topLevelTypeConstructors).toSet
      case _ => Set(tp)
    }
  }

  private def complexity(tp: ScType): Int =
    tp match {
      case ScProjectionType(proj, _)     => 1 + complexity(proj)
      case ParameterizedType(_, args)    => 1 + args.foldLeft(0)(_ + complexity(_))
      case ScDesignatorType(_: ScObject) => 1
      case ScDesignatorType(v: ScTypedDefinition) =>
        val valueType: ScType = v.`type`().getOrAny
        1 + complexity(valueType)
      case ScCompoundType(comps, _, _) => comps.foldLeft(0)(_ + complexity(_))
      case _                           => 1
    }

  private def checkWeakConformance(left: ScType, right: ScType): Boolean = {
    def function1Arg(scType: ScType): Option[(ScType, ScType)] = scType match {
      case ParameterizedType(ScDesignatorType(c: PsiClass), args) if args.size == 2 =>
        if (c.qualifiedName == "scala.Function1") (args.head, args.last).toOption
        else                                      None
      case _ => None
    }

    function1Arg(left) match {
      case Some((leftArg, leftRes)) =>
        function1Arg(right) match {
          case Some((rightArg, rightRes)) => rightArg.weakConforms(leftArg) && leftRes.conforms(rightRes)
          case _                          => false
        }
      case _ => false
    }
  }
}
