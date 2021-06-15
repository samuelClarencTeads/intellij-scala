package org.jetbrains.plugins.scala.lang.formatting.scalafmt.dynamic

import java.lang.reflect.Constructor

import org.jetbrains.plugins.scala.lang.formatting.scalafmt.dynamic.exceptions.ReflectionException
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.dynamic.utils.ReflectUtils.*

import scala.util.Try

//noinspection TypeAnnotation,HardCodedStringLiteral
class ScalafmtReflectConfig private[dynamic](
  val fmtReflect: ScalafmtReflect,
  private[dynamic] val target: Object, // real config object
  private val classLoader: ClassLoader
) {

  private val targetCls = target.getClass
  private val constructor: Constructor[?] = targetCls.getConstructors()(0)
  private val constructorParamNames = constructor.getParameters.map(_.getName)
  private val publicMethodNames = targetCls.getMethods.map(_.getName)
  private val rewriteParamIdx = constructorParamNames.indexOf("rewrite").ensuring(_ >= 0)
  private val emptyRewrites = target.invoke("apply$default$" + (rewriteParamIdx + 1))

  private val dialectCls  = classLoader.loadClass("scala.meta.Dialect")
  private val dialectsCls = classLoader.loadClass("scala.meta.dialects.package")

  private val rewriteRulesMethod = Try(targetCls.getMethod("rewrite")).toOption

  private val continuationIndentMethod         = Try(targetCls.getMethod("continuationIndent")).toOption
  private val continuationIndentCallSiteMethod = Try(targetCls.getMethod("continuationIndentCallSite")).toOption
  private val continuationIndentDefnSiteMethod = Try(targetCls.getMethod("continuationIndentDefnSite")).toOption

  private val DefaultIndentCallSite = 2
  private val DefaultIndentDefnSite = 4

  private val sbtDialect: Object =
    try dialectsCls.invokeStatic("Sbt") catch {
      case ReflectionException(_: NoSuchMethodException) =>
        dialectsCls.invokeStatic("Sbt0137")
    }

  val version: String =
    target.invokeAs[String]("version").trim

  def isIncludedInProject(filename: String): Boolean = {
    val matcher = target.invoke("project").invoke("matcher")
    matcher.invokeAs[java.lang.Boolean]("matches", filename.asParam)
  }

  def withSbtDialect: ScalafmtReflectConfig = {
    // TODO: maybe hold loaded classes in some helper class not to reload them each time?
    val newTarget = target.invoke("withDialect", (dialectCls, sbtDialect))
    new ScalafmtReflectConfig(fmtReflect, newTarget, classLoader)
  }

  // TODO: what about rewrite tokens?
  def hasRewriteRules: Boolean =
    rewriteRulesMethod match {
      case Some(method) =>
        // > v0.4.1
        val rewriteSettings = method.invoke(target)
        !rewriteSettings.invoke("rules").invokeAs[Boolean]("isEmpty")
      case None =>
        false
    }

  def withoutRewriteRules: ScalafmtReflectConfig =
    if (hasRewriteRules) {
      val fieldsValues: Array[Object] = constructorParamNames.zipWithIndex.map { case (param, idx) =>
        // some public case class fields were made deprecated and made private, so we can't access them
        // https://github.com/scalameta/scalafmt/commit/581a99660373554468617b27a349dc732aff92e2
        // https://github.com/scalameta/scalafmt/commit/5bf5fbfc4454131b113731880002f52c725512c1
        val accessorName = if (publicMethodNames.contains(param)) param else param + ("$access$" + idx)
        target.invoke(accessorName)
      }
      fieldsValues(rewriteParamIdx) = emptyRewrites
      val targetNew = constructor.newInstance(fieldsValues*).asInstanceOf[Object]
      new ScalafmtReflectConfig(fmtReflect, targetNew, classLoader)
    } else {
      this
    }

  val continuationIndentCallSite: Int =
    continuationIndentMethod match {
      case Some(method) => // >v0.4
        val indentsObj = method.invoke(target)
        indentsObj.invokeAs[Int]("callSite")
      case None =>
        continuationIndentCallSiteMethod match {
          case Some(method) => // >v0.2.0
            method.invoke(target).asInstanceOf[Int]
          case None =>
            DefaultIndentCallSite
        }
    }

  val continuationIndentDefnSite: Int =
    continuationIndentMethod match {
      case Some(method) =>
        val indentsObj = method.invoke(target)
        indentsObj.invokeAs[Int]("defnSite")
      case None =>
        continuationIndentDefnSiteMethod match {
          case Some(method) =>
            method.invoke(target).asInstanceOf[Int]
          case None =>
            DefaultIndentDefnSite
        }
    }

  override def equals(obj: Any): Boolean = target.equals(obj)

  override def hashCode(): Int = target.hashCode()
}