package org.jetbrains.sbt;

import com.intellij.DynamicBundle
import org.jetbrains.annotations.{Nls, PropertyKey}
import org.jetbrains.plugins.scala.NlsString

import scala.annotation.varargs

object SbtBundle {
    private final val BUNDLE = "messages.ScalaSbtBundle"
    private object INSTANCE extends DynamicBundle(BUNDLE)

    //noinspection ReferencePassedToNls
    @Nls
    @varargs
    def message(@PropertyKey(resourceBundle = BUNDLE) key: String, params: Any*): String =
        INSTANCE.getMessage(key, params.map(_.toString)*)

    //noinspection ReferencePassedToNls
    @Nls
    @varargs
    def nls(@PropertyKey(resourceBundle = BUNDLE) key: String, params: Any*): NlsString =
        NlsString(INSTANCE.getMessage(key, params.map(_.toString)*))
}
