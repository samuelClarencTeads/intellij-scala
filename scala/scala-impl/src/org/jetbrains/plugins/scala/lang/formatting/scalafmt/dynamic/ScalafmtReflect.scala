package org.jetbrains.plugins.scala.lang.formatting.scalafmt.dynamic

import java.lang.reflect.Method
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import org.jetbrains.plugins.scala.lang.formatting.scalafmt.dynamic.exceptions.*
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.dynamic.utils.ReflectUtils.*

import scala.util.Try

//noinspection TypeAnnotation
case class ScalafmtReflect(
  classLoader: URLClassLoader,
  version: String,
  respectVersion: Boolean
) {

  import classLoader.loadClass

  // FIXME: the class does not exist for version old versions, e.g. v0.2.8
  private val formattedCls = loadClass("org.scalafmt.Formatted")
  private val scalaSetCls  = loadClass("scala.collection.immutable.Set")
  private val optionCls    = loadClass("scala.Option")
  private val configCls    = loadClass("org.scalafmt.config.Config")
  private val scalafmtCls  = loadClass("org.scalafmt.Scalafmt")

  private val parseExceptionCls    = loadClass("scala.meta.parsers.ParseException")
  private val tokenizeExceptionCls = loadClass("scala.meta.tokenizers.TokenizeException")

  private val defaultScalaFmtConfig = scalafmtCls.invokeStatic("format$default$2")
  private val emptyRange            = scalafmtCls.invokeStatic("format$default$3")

  private val formattedGet = formattedCls.getMethod("get")

  private val formatMethod: Method =
    scalafmtCls.getMethod("format", classOf[String], defaultScalaFmtConfig.getClass, scalaSetCls)
  private val formatMethodWithFilename: Option[Method] = Try(
    scalafmtCls.getMethod("format", classOf[String], defaultScalaFmtConfig.getClass, scalaSetCls, classOf[String])
  ).toOption

  lazy val intellijScalaFmtConfig: ScalafmtReflectConfig = {
    // TODO: see implementation details for other versions of scalafmt, find where intellij config is kept
    assert(version == "1.5.1", "intellij scalafmt config is only supported fot version 1.5.1 for now")

    val scalaFmtConfigCls = classLoader.loadClass("org.scalafmt.config.ScalafmtConfig")
    val configTarget = scalaFmtConfigCls.invokeStatic("intellij")
    new ScalafmtReflectConfig(this, configTarget, classLoader)
  }

  def parseConfig(configPath: Path): ScalafmtReflectConfig = {
    val configText = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8)
    parseConfigFromString(configText)
  }

  def parseConfigFromString(configText: String): ScalafmtReflectConfig = {
    try {
      val configured: Object = try { // scalafmt >= 1.6.0
        scalafmtCls.invokeStatic("parseHoconConfig", configText.asParam)
      } catch {
        case _: NoSuchMethodException =>
          // scalafmt >= v0.7.0-RC1 && scalafmt < 1.6.0
          val fromHoconEmptyPath = configCls.invokeStatic("fromHoconString$default$2")
          configCls.invokeStatic("fromHoconString", configText.asParam, (optionCls, fromHoconEmptyPath))
      }
      new ScalafmtReflectConfig(this, configured.invoke("get"), classLoader)
    } catch {
      case ReflectionException(e) =>
        throw ScalafmtConfigException(e.getMessage)
    }
  }

  def format(code: String, config: ScalafmtReflectConfig, filenameOpt: Option[String]): String = {
    checkVersionMismatch(config)
    val formatted = (formatMethodWithFilename, filenameOpt) match {
      case (Some(method), Some(filename)) =>
        method.invoke(null, code, config.target, emptyRange, filename)
      case _ =>
        formatMethod.invoke(null, code, config.target, emptyRange)
    }
    clearTokenizerCache()
    try
      formattedGet.invoke(formatted).asInstanceOf[String]
    catch {
      case ReflectionException(e)
        if tokenizeExceptionCls.isInstance(e) ||
          parseExceptionCls.isInstance(e) =>
        val pos = e.invoke("pos")
        val range = positionRange(pos)
        val shortMessage = e.invokeAs[String]("shortMessage")
        throw PositionExceptionImpl(filenameOpt, code, shortMessage, e.getMessage, range, e)
    }
  }

  private def positionRange(pos: Object): RangePosition =
    try {
      RangePosition(
        pos.invokeAs[Int]("start"),
        pos.invokeAs[Int]("startLine"),
        pos.invokeAs[Int]("startColumn"),
        pos.invokeAs[Int]("end"),
        pos.invokeAs[Int]("endLine"),
        pos.invokeAs[Int]("endColumn")
      )
    } catch {
      case _: ReflectiveOperationException | _: ClassCastException =>
        val start = pos.invoke("start")
        val end = pos.invoke("end")
        RangePosition(
          start.invokeAs[Int]("offset"),
          start.invokeAs[Int]("line"),
          start.invokeAs[Int]("column"),
          end.invokeAs[Int]("offset"),
          end.invokeAs[Int]("line"),
          end.invokeAs[Int]("column")
        )
    }

  private def clearTokenizerCache(): Unit = {
    val cache = moduleInstance(
      "scala.meta.internal.tokenizers.PlatformTokenizerCache$"
    )
    cache.invoke("megaCache").invoke("clear")
  }

  private def checkVersionMismatch(config: ScalafmtReflectConfig): Unit =
    if (respectVersion) {
      val obtained = config.version
      if (obtained != version) {
        throw VersionMismatch(obtained, version)
      }
    }

  private def moduleInstance(fqn: String): Object = {
    val cls = classLoader.loadClass(fqn)
    val module = cls.getField("MODULE$")
    module.setAccessible(true)
    module.get(null)
  }
}
