package org.jetbrains.plugins.scala
package worksheet.ui

import java.util

import com.intellij.openapi.editor.ex.{FoldingListener, FoldingModelEx}
import com.intellij.openapi.editor.{Document, Editor, FoldRegion}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.FileAttribute
import org.jetbrains.plugins.scala.macroAnnotations.Measure
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.worksheet.ui.WorksheetDiffSplitters.{DiffMapping, SimpleWorksheetSplitter}
import org.jetbrains.plugins.scala.worksheet.ui.WorksheetFoldGroup.*
import org.jetbrains.plugins.scala.worksheet.utils.FileAttributeUtilCache

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

final class WorksheetFoldGroup(
  private val viewerEditor: Editor, // left editor
  private val originalEditor: Editor, // right editor
  project: Project,
  private val splitter: Option[SimpleWorksheetSplitter]
) {

  import FoldRegionSerializer.*

  private val originalDocument: Document = originalEditor.getDocument
  private val viewerDocument  : Document = viewerEditor.getDocument
  
  private val _regions = mutable.ArrayBuffer[FoldRegionInfo]()
  private val unfolded = new util.TreeMap[Int, Int]()

  def foldedLinesCount: Int = _regions.map(_.spaces).sum

  def expandedRegionsIndexes: Seq[Int] = _regions.iterator.zipWithIndex.filter(_._1.expanded).map(_._2).toSeq

  def left2rightOffset(left: Int): Int = {
    val key: Int = unfolded floorKey left

    if (key == 0) {
      left
    } else {
      left + unfolded.get(key)
    }
  }

  /**
   * @param foldStartOffset start of the range to fold in the viewerEditor
   * @param foldEndOffset   end of the range to fold in the viewerEditor
   * @param leftEndOffset    end of the current input content from the  originalEditor
   * @param leftContentLines number of lines of the current input content from the originalEditor
   * @param spaces           number of folded lines - 1 in the viewerEditor (number of new line characters folded)
   * @param isExpanded       whether the region should be expanded right after folding
   */
  def addRegion(foldingModel: FoldingModelEx)
               (foldStartOffset: Int, foldEndOffset: Int,
                leftEndOffset: Int,
                leftContentLines: Int,
                spaces: Int,
                isExpanded: Boolean): Unit = {
    val placeholder: String = {
      val foldedTextLength = foldEndOffset - foldStartOffset
      val length = Math.min(foldedTextLength, WorksheetFoldGroup.PLACEHOLDER_LIMIT)
      val range = TextRange.from(foldStartOffset, length)
      viewerDocument.getText(range)
    }

    val region = foldingModel.createFoldRegion(foldStartOffset, foldEndOffset, placeholder, null, false)
    if (region == null) return //something went wrong

    region.setExpanded(isExpanded)
    _regions += FoldRegionInfo(region, leftEndOffset, leftContentLines, spaces, region.isExpanded)
  }

  def clearRegions(): Unit = {
    _regions.clear()
    unfolded.clear()
    splitter.foreach(_.clear())
  }

  private def addParsedRegions(regions: Seq[ParsedRegion]): Unit = {
    val folding = viewerEditor.getFoldingModel.asInstanceOf[FoldingModelEx]
    folding.runBatchFoldingOperation { () =>
      regions.foreach(addParsedRegion(folding, _))
    }
  }

  private def addParsedRegion(folding: FoldingModelEx, region: ParsedRegion): Unit = {
    val ParsedRegion(start, end, leftEndLine, leftSideLength, spaces, expanded) = region
    addRegion(folding)(start, end, leftEndLine, leftSideLength, spaces, expanded)
  }

  def expand(regionIdx: Int): Boolean =
    _regions.lift(regionIdx) match {
      case Some(region) => expand(region.region)
      case None         => false
    }

  private def expand(region: FoldRegion): Boolean =
    traverseAndChange(region, expand = true)

  private def collapse(region: FoldRegion): Boolean =
    traverseAndChange(region, expand = false)

  def installOn(model: FoldingModelEx): Unit =
    model.addListener(new WorksheetFoldRegionListener(this), project.unloadAwareDisposable)

  def initMappings(): Unit = {
    val (mappings, _, _) = traverseRegions(null)
    splitter.foreach(_.update(mappings))
    _regions.iterator
      .filter(_.expanded)
      .foreach(updateChangeFolded(_, expand = true))
  }

  private def traverseAndChange(target: FoldRegion, expand: Boolean): Boolean = {
    val (mappings, targetInfo, _) = traverseRegions(target)

    if (targetInfo == null || targetInfo.expanded == expand) return false

    splitter.foreach(_.update(mappings))

    targetInfo.expanded = expand

    updateChangeFolded(targetInfo, expand)
    true
  }

  @Measure
  private def traverseRegions(target: FoldRegion): (Iterable[DiffMapping], FoldRegionInfo, Int) = {
    val emptyResult: (Seq[DiffMapping], FoldRegionInfo, Int) = (Seq.empty, null, 0)
    if (_regions.isEmpty) return emptyResult

    def numbers(reg: FoldRegionInfo, stored: Int): DiffMapping = {
      val leftEndOffset = reg.leftEndOffset - 1
      val leftEndLine   = originalDocument.safeLineNumber(leftEndOffset)
      val leftStartLine = leftEndLine - reg.leftContentLines + 1

      DiffMapping(leftStartLine, leftEndLine, leftEndLine + stored, reg.spaces)
    }

    _regions.foldLeft(emptyResult) { case (acc@(res, currentRegion, offset), nextRegion) =>
      val accNew = if (nextRegion.region == target) {
        if (nextRegion.expanded) {
          (res, nextRegion, offset)
        } else {
          val resUpdated = res :+ numbers(nextRegion, offset)
          (resUpdated, nextRegion, offset + nextRegion.spaces)
        }
      } else if (nextRegion.expanded) {
        val resUpdated = res :+ numbers(nextRegion, offset)
        (resUpdated, currentRegion, offset + nextRegion.spaces)
      } else {
        acc
      }
      accNew
    }
  }

  private def updateChangeFolded(target: FoldRegionInfo, expand: Boolean): Unit = {
    val line = originalDocument.safeLineNumber(target.leftEndOffset - 1)
    val key = unfolded floorKey line

    val spaces = target.spaces
    if (unfolded.get(key) == 0) {
      if (expand) unfolded.put(line, spaces)
      else unfolded.remove(line)
      return
    }

    val lower = unfolded.tailMap(line).entrySet().iterator()
    while (lower.hasNext) {
      val t = lower.next()
      val magicValue = if (expand) t.getValue + spaces else t.getValue - spaces
      unfolded.put(t.getKey, magicValue)
    }

    if (expand) {
      unfolded.put(line, unfolded.get(key) + spaces)
    } else {
      unfolded.remove(line)
    }
  }
}

object WorksheetFoldGroup {

  import FoldRegionSerializer.*

  private val PLACEHOLDER_LIMIT = 75
  private val WORKSHEET_PERSISTENT_FOLD_KEY = new FileAttribute("WorksheetPersistentFoldings", 1, false)

  /**
   * @param leftEndOffset    end offset of corresponding input text range (exclusive)
   * @param leftContentLines number of lines on corresponding input text
   * @param spaces           number of folded lines
   * @param expanded         whether the region is expanded
   */
  private case class FoldRegionInfo(region: FoldRegion,
                                    leftEndOffset: Int,
                                    leftContentLines: Int,
                                    spaces: Int,
                                    var expanded: Boolean) {
    override def equals(obj: scala.Any): Boolean = obj match {
      case info: FoldRegionInfo => this.region.equals(info.region)
      case _ => false
    }

    override def hashCode(): Int = region.hashCode()
  }

  // TODO: looks like listeners are leaked, see FoldingModelImpl.notifyListenersOnFoldRegionStateChange
  private class WorksheetFoldRegionListener(val owner: WorksheetFoldGroup) extends FoldingListener {
    override def onFoldRegionStateChange(region: FoldRegion): Unit =
      if (region.isExpanded) owner.expand(region)
      else owner.collapse(region)

    override def onFoldProcessingEnd(): Unit = {}
  }

  /**
   * Used in [[org.jetbrains.plugins.scala.worksheet.actions.topmenu.CopyWorksheetAction]]
   * to copy text with its output attached as comments to the right of the input (if output is available)
   *
   * @return Seq(input line index -> output line index)
   *         NOTE: the mapping is returned starting from the first element which output was folded
   */
  private def extractMappings(parsedRegions: Seq[ParsedRegion],
                              originalDocument: Document,
                              viewerDocument: Document): Seq[(Int, Int)] = {
    if (parsedRegions.isEmpty || viewerDocument.getLineCount == 0) return Seq()

    val builder = Seq.newBuilder[(Int, Int)]

    val Seq(parsedHead, parsedTail*) = parsedRegions
    val regionsEffective = parsedTail :+ fakeEndFoldRegion(originalDocument, viewerDocument)

    regionsEffective.foldLeft(parsedHead) { case (prevFolding, currFolding) =>
      val prevLeftEndLine = originalDocument.safeLineNumber(prevFolding.leftEndOffset - 1)
      val prevRightStartLine = viewerDocument.safeLineNumber(prevFolding.foldStartOffset)

      builder += ((prevLeftEndLine, prevRightStartLine))

      val currLeftEndLine = originalDocument.safeLineNumber(currFolding.leftEndOffset - 1)
      val currLeftStartLine = currLeftEndLine - currFolding.leftSideLength + 1
      val linesBetween = currLeftStartLine - prevLeftEndLine - 1
      if (linesBetween > 0) {
        val prevRightEndLine  = viewerDocument.safeLineNumber(prevFolding.foldEndOffset)
        val mappingsBetween = (1 to linesBetween).map { idx =>
          (prevLeftEndLine + idx, prevRightEndLine + idx)
        }
        builder ++= mappingsBetween
      }

      currFolding
    }
    
    builder.result()
  }
  
  private def fakeEndFoldRegion(originalDocument: Document, viewerDocument: Document): ParsedRegion = {
    val viewerLength = viewerDocument.getTextLength - 1
    val originalLength = originalDocument.getTextLength - 1
    ParsedRegion(
      foldStartOffset = viewerLength,
      foldEndOffset = viewerLength,
      expanded = false,
      leftEndOffset = originalLength,
      spaces = 0,
      leftSideLength = 1
    )
  }

  implicit class DocumentExt(private val document: Document) extends AnyVal {
    def safeLineNumber(offset: Int): Int =
      document.getLineNumber(offset.min(document.getTextLength))
  }

  def save(file: VirtualFile, group: WorksheetFoldGroup): Unit = {
    if (!file.isValid) return

    val regionsSerialized = serializeFoldRegions(group._regions)
    FileAttributeUtilCache.writeAttribute(WORKSHEET_PERSISTENT_FOLD_KEY, file, regionsSerialized)
  }

  def load(
    viewerEditor: Editor,
    originalEditor: Editor,
    project: Project,
    splitter: SimpleWorksheetSplitter,
    file: VirtualFile
  ): WorksheetFoldGroup = {
    val group = new WorksheetFoldGroup(viewerEditor, originalEditor, project, Some(splitter))

    val parsedRegions = extractRegions(file)
    parsedRegions.foreach(group.addParsedRegions)

    group
  }
  
  def computeMappings(viewerEditor: Editor, originalEditor: Editor, file: VirtualFile): Seq[(Int, Int)] = {
    val parsedRegions = extractRegions(file).getOrElse(Seq())
    val mappings = extractMappings(parsedRegions, originalEditor.getDocument, viewerEditor.getDocument)
    mappings.headOption match {
      case Some((_, 0)) => mappings
      case _ => (0, 0) +: mappings
    }
  }

  private def extractRegions(file: VirtualFile): Option[Seq[ParsedRegion]] = {
    val regionsSerialized = FileAttributeUtilCache.readAttribute(WORKSHEET_PERSISTENT_FOLD_KEY, file).filter(_.nonEmpty)
    regionsSerialized.map(deserializeFoldRegions)
  }

  private object FoldRegionSerializer {

    private val FieldSeparator = ','
    private val RegionsSeparator = '|'

    case class ParsedRegion(foldStartOffset: Int,
                            foldEndOffset: Int,
                            leftEndOffset: Int,
                            leftSideLength: Int,
                            spaces: Int,
                            expanded: Boolean)

    def serializeFoldRegions(regions: Iterable[FoldRegionInfo]): String = {
      val regionsSerialized = regions.map {
        case FoldRegionInfo(region, leftEndOffset, leftContentLines, spaces, expanded) =>
          val fields = Seq(region.getStartOffset, region.getEndOffset, expanded, leftEndOffset, spaces, leftContentLines)
          fields.mkString(FieldSeparator.toString)
      }
      regionsSerialized.mkString(RegionsSeparator.toString)
    }

    def deserializeFoldRegions(text: String): Seq[ParsedRegion] = {
      val regionsDumps  = text.split(RegionsSeparator)
      val regionsFields = regionsDumps.iterator.map(_.split(FieldSeparator))
      regionsFields.collect {
        case Array(start, end, expanded, leftEndLine, spaces, leftSideLength) =>
          ParsedRegion(start.toInt, end.toInt, leftEndLine.toInt, leftSideLength.toInt, spaces.toInt, expanded == "true")
      }.toSeq
    }
  }
}
