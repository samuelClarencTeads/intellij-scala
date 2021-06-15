package org.jetbrains.plugins.scala
package lang
package refactoring

import java.awt.datatransfer.DataFlavor

import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.openapi.util.TextRange

abstract class AssociationsData(val associations: Array[Association],
                                private val companion: AssociationsData.Companion[?]) extends TextBlockTransferableData {

  import AssociationsData.*

  override final def getOffsetCount: Int = 2 * associations.length

  override final def getOffsets(offsets: Array[Int], startIndex: Int): Int =
    this (offsets, startIndex) {
      case (array, index) => array(index) = associations(index).range
    }

  override final def setOffsets(offsets: Array[Int], startIndex: Int): Int =
    this (offsets, startIndex) {
      case (array, index) => associations(index).range = array(index)
    }

  override final def getFlavor: DataFlavor = companion.flavor

  private def apply(offsets: Array[Int], startIndex: Int)
                   (action: (OffsetsArray, Int) => Unit) = {
    val array = new OffsetsArray(offsets, startIndex)

    for {
      index <- associations.indices
    } action(array, index)

    startIndex + getOffsetCount
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AssociationsData]

  override def equals(other: Any): Boolean = other match {
    case that: AssociationsData if that.canEqual(this) => associations.sameElements(that.associations)
    case _ => false
  }

  override def hashCode: Int = associations.hashCode
}

object AssociationsData {

  abstract class Companion[D <: AssociationsData](representationClass: Class[D],
                                                  humanPresentableName: String) {
    lazy val flavor = new DataFlavor(representationClass, humanPresentableName)
  }

  private class OffsetsArray(private val offsets: (Array[Int], Int)) extends AnyVal {

    def apply(index: Int): TextRange = {
      val (offsets, offset) = startIndex(index)

      TextRange.create(
        offsets(offset),
        offsets(offset + 1)
      )
    }

    def update(index: Int, range: TextRange): Unit = {
      val (offsets, offset) = startIndex(index)

      offsets(offset) = range.getStartOffset
      offsets(offset + 1) = range.getEndOffset
    }

    private def startIndex(index: Int) = {
      val (offsets, startIndex) = this.offsets
      (offsets, startIndex + 2 * index)
    }
  }

}
