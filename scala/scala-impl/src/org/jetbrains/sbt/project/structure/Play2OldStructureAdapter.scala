package org.jetbrains.sbt.project.structure

import java.io.File

import org.jetbrains.sbt.project.data.Play2ProjectData
import org.jetbrains.sbt.project.structure.Play2Keys.AllKeys.*
import org.jetbrains.sbt.structure.Play2Data
import org.jetbrains.sbt.RichSeq

import scala.collection.immutable.HashMap

/**
  * @author Nikolay Obedin
  */
// TODO: @dmitry.naydanov: please, refactor Play2 part and then remove this class
object Play2OldStructureAdapter {
  type ProjectId = String

  def apply(newData: Seq[(ProjectId, File, Play2Data)]): Play2ProjectData = {
    val projectKeyValueTriples = newData.flatMap {
      case (id, baseDir, data) => extractProjectKeyValue(id, baseDir, data)
    }
    val oldData = projectKeyValueTriples.groupBy(_._2).map {
      case (string, triples) => (string, triples.map(t => (t._1, t._3)))
    }

    Play2ProjectData(avoidSL7005Bug[String, ProjectId, ParsedValue[?]](oldData))
  }

  private def extractProjectKeyValue(id: ProjectId, baseDir: File, data: Play2Data): Seq[(ProjectId, String, ParsedValue[?])] =  {
    val playVersion = data.playVersion.map(v => (PLAY_VERSION, new StringParsedValue(v))).toSeq
    val confDirectory = data.confDirectory.map(d => (PLAY_CONF_DIR, new StringParsedValue(d.getCanonicalPath))).toSeq

    val keyValues = playVersion ++ confDirectory ++ Seq(
      (TEMPLATES_IMPORT, new SeqStringParsedValue(data.templatesImports.toJavaList)),
      (ROUTES_IMPORT, new SeqStringParsedValue(data.routesImports.toJavaList)),
      (SOURCE_DIR, new StringParsedValue(data.sourceDirectory.getCanonicalPath)),
      (PROJECT_URI, new StringParsedValue(baseDir.getCanonicalFile.toURI.toString))
    )

    keyValues.map({ case (k, v) => (id, k.name, v)})
  }

  //SCL-7005
  @inline private def avoidSL7005Bug[K, A, B](m: Map[K, Seq[(A, B)]]): Map[K, Map[A, B]] = {
    val withMapsValues = m.view.mapValues(_.toMap).toMap
    HashMap(withMapsValues.toSeq*)
  }
}
