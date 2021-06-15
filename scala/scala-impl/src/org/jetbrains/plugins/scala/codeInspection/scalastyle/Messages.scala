package org.jetbrains.plugins.scala.codeInspection.scalastyle

import java.text.MessageFormat
import java.util.Properties

import scala.util.Using

object Messages {

  private lazy val messages =
    Using(getClass.getResourceAsStream("/reference.conf")) { conf =>
      val props = new Properties()
      props.load(conf)
      props
    }.getOrElse(new Properties())

  def format(key: String, args: List[String], customMessage: Option[String]): String = {
    try {
      val rawMessage =
        Option(messages.getProperty(s"$key.message"))
          .getOrElse(messages.getProperty(key))
      val message = rawMessage.substring(0, rawMessage.length - 1).substring(1)
      MessageFormat.format(message, args*)
    } catch {
      case _: Throwable => customMessage.getOrElse("")
    }
  }

}
