package org.jetbrains.plugins.scala.lang.formatting.scalafmt.dynamic.utils

import scala.reflect.ClassTag

object ReflectUtils {

  implicit class ObjectReflectOps[T](private val target: T) extends AnyVal {
    def invokeAs[R](methodName: String, args: (Class[?], Object)*): R = {
      invoke(methodName, args*).asInstanceOf[R]
    }

    def invoke(methodName: String, args: (Class[?], Object)*): Object = {
      val clazz = target.getClass
      val method = clazz.getMethod(methodName, args.map(_._1)*)
      method.invoke(target, args.map(_._2)*)
    }

    def asParam(implicit classTag: ClassTag[T]): (Class[?], T) = {
      (classTag.runtimeClass, target)
    }
  }

  implicit class ClassReflectOps(private val clazz: Class[?]) extends AnyVal {
    def invokeStaticAs[T](methodName: String, args: (Class[?], Object)*): T = {
      invokeStatic(methodName, args*).asInstanceOf[T]
    }

    def invokeStatic(methodName: String, args: (Class[?], Object)*): Object = {
      val method = clazz.getMethod(methodName, args.map(_._1)*)
      method.invoke(null, args.map(_._2)*)
    }
  }

}
