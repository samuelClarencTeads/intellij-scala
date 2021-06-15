package org.jetbrains.plugins.scala

import com.intellij.openapi.extensions.ExtensionPointName
import scala.jdk.CollectionConverters.*

/**
  * Handy base class for declaring extension points.
  */
abstract class ExtensionPointDeclaration[T](private val name: String) {
  private val extensionPointName = ExtensionPointName.create[T](name)

  def implementations: Seq[T] = {
    extensionPointName.getExtensionList.asScala.toSeq
  }
}
