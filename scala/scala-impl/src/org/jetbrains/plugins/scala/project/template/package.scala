package org.jetbrains.plugins.scala
package project

import java.io.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.stream

import com.intellij.execution.process.{OSProcessHandler, ProcessAdapter, ProcessEvent}
import com.intellij.openapi.util.{Key, io}
import com.intellij.openapi.vfs.{VfsUtil, VirtualFile, VirtualFileManager}
import com.intellij.util.{PathUtil, net}

import scala.util.Using

/**
 * @author Pavel Fatin
 */
package object template {

  import io.FileUtil.*

  def usingTempFile[T](prefix: String, suffix: String = null)(block: File => T): T = {
    val file = createTempFile(prefix, suffix, true)
    try {
      block(file)
    } finally {
      file.delete()
    }
  }

  def usingTempDirectory[T](prefix: String)(block: File => T): T = {
    val directory = createTempDirectory(prefix, null, true)
    try {
      block(directory)
    } finally {
      delete(directory)
    }
  }

  def writeLinesTo(file: File)
                  (lines: String*): Unit = {
    Using.resource(new PrintWriter(new FileWriter(file))) { writer =>
      lines.foreach(writer.println)
      writer.flush()
    }
  }

  implicit class PathExt(path: Path) {
    def /(string: String): Path = path.resolve(string)
    def walk: stream.Stream[Path] = Files.walk(path)
    def children: stream.Stream[Path] = Files.list(path)
    def exists: Boolean = Files.exists(path)
    def childExists(sub: String): Boolean = Files.exists(path / sub)
    def isDir: Boolean = Files.isDirectory(path)
    def nameContains(str: String): Boolean = path.getFileName.toString.contains(str)
  }

  implicit class FileExt(private val delegate: File) extends AnyVal {
    def /(path: String): File = new File(delegate, path)

    def /(paths: Seq[String]): File = paths.foldLeft(delegate)(_ / _)

    def parent: Option[File] = Option(delegate.getParentFile)

    def children: Seq[File] = Option(delegate.listFiles).map(_.toSeq).getOrElse(Seq.empty)

    def childrenNames: Seq[String] = children.map(_.getName)

    def directories: Seq[File] = children.filter(_.isDirectory)

    def files: Seq[File] = children.filter(_.isFile)

    def findByName(name: String): Option[File] = Some(new File(delegate, name)).filter(_.exists())

    def childExists(name: String): Boolean = new File(delegate, name).exists()

    def allFiles: LazyList[File] = {
      val (files, directories) = children.to(LazyList).span(_.isFile)
      files #::: directories.flatMap(_.allFiles)
    }

    def toLibraryRootURL: String = VfsUtil.getUrlForLibraryRoot(delegate)

    def toVirtualFile: Option[VirtualFile] = {
      val url = URLDecoder.decode(delegate.toPath.toUri.toString, StandardCharsets.UTF_8.name())
      Option(VirtualFileManager.getInstance.findFileByUrl(url))
    }
  }
}
