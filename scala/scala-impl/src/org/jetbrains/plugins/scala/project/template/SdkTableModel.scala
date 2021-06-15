package org.jetbrains.plugins.scala
package project
package template

import java.lang.{Boolean as JBoolean}

import com.intellij.util.ui.{ColumnInfo, ListTableModel}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.ScalaBundle

/**
 * @author Pavel Fatin
 */
final class SdkTableModel extends ListTableModel[SdkChoice](
  new ColumnInfo[SdkChoice, String](ScalaBundle.message("sdk.table.model.location")) {
    override def valueOf(item: SdkChoice): String = item.source

    override def getPreferredStringValue = "Maven"
  },
  new ColumnInfo[SdkChoice, String](ScalaBundle.message("sdk.table.model.version")) {

    import Version.*

    override def valueOf(item: SdkChoice): String = item.sdk.version.fold(Default)(abbreviate)

    override def getPreferredStringValue = "2.11.0"
  },
  new SdkTableModel.BooleanColumnInfo(ScalaBundle.message("sdk.table.model.sources")) {
    override def valueOf(item: SdkChoice): JBoolean = item.sdk.sourceFiles.nonEmpty
  },
  new SdkTableModel.BooleanColumnInfo(ScalaBundle.message("sdk.table.model.docs")) {
    override def valueOf(item: SdkChoice): JBoolean = item.sdk.docFiles.nonEmpty
  })

object SdkTableModel {

  private abstract class BooleanColumnInfo(@Nls name: String) extends ColumnInfo[SdkChoice, JBoolean](name) {
    override final def getColumnClass: Class[JBoolean] = classOf[JBoolean]

    override final def getPreferredStringValue = "0"
  }

}