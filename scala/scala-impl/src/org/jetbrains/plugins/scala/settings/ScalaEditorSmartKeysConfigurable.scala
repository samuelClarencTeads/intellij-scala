package org.jetbrains.plugins.scala.settings

import java.awt.GridLayout

import com.intellij.openapi.options.{BeanConfigurable, SearchableConfigurable}
import com.intellij.util.ui.JBUI
import javax.swing.*
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.ObjectExt

class ScalaEditorSmartKeysConfigurable extends BeanConfigurable[ScalaApplicationSettings](ScalaApplicationSettings.getInstance) with SearchableConfigurable {
  override def getId: String = "ScalaSmartKeys"
  //noinspection ScalaExtractStringToBundle
  override def getDisplayName: String = "Scala"
  override def getHelpTopic: String = null
  override def enableSearch(option: String): Runnable = null
  override def disposeUIResources(): Unit = super.disposeUIResources()

  init()

  def init(): Unit = {
    val settings: ScalaApplicationSettings = getInstance();
    setTitle("Scala")
    checkBox(ScalaBundle.message("insert.pair.multiline.quotes"), () => settings.INSERT_MULTILINE_QUOTES, settings.INSERT_MULTILINE_QUOTES = _)
    checkBox(ScalaBundle.message("upgrade.to.interpolated"), () => settings.UPGRADE_TO_INTERPOLATED, settings.UPGRADE_TO_INTERPOLATED = _)
    checkBox(ScalaBundle.message("wrap.single.expression.body"), () => settings.WRAP_SINGLE_EXPRESSION_BODY, settings.WRAP_SINGLE_EXPRESSION_BODY = _)
    checkBox(ScalaBundle.message("insert.block.braces.automatically.based.on.indentation"), () => settings.HANDLE_BLOCK_BRACES_INSERTION_AUTOMATICALLY, settings.HANDLE_BLOCK_BRACES_INSERTION_AUTOMATICALLY = _)
    checkBox(ScalaBundle.message("remove.block.braces.automatically.based.on.indentation"), () => settings.HANDLE_BLOCK_BRACES_REMOVAL_AUTOMATICALLY, settings.HANDLE_BLOCK_BRACES_REMOVAL_AUTOMATICALLY = _)

  }

  override def createComponent: JComponent = {
    val result = super.createComponent
    assert(result != null, "ScalaEditorSmartKeysConfigurable panel was not created")

    // Group the auto-brace options by padding them to the right and adding a caption
    val isAutoBraceOptionTitle = Set(
      ScalaBundle.message("insert.block.braces.automatically.based.on.indentation"),
      ScalaBundle.message("remove.block.braces.automatically.based.on.indentation"),
    )

    for {
      // get the panel
      panel <- result.getComponent(0).asOptionOf[JComponent]
      // get the auto-brace options
      panelComponents = panel.getComponents
      autoBraceOptions = panelComponents.collect { case jbox: JCheckBox if isAutoBraceOptionTitle(jbox.getText) => jbox }
      if autoBraceOptions.nonEmpty
      gridLayout <- panel.getLayout.asOptionOf[GridLayout]
    } {
      // add a label above the auto-brace options
      gridLayout.setRows(gridLayout.getRows + 1)
      val groupCaptionPosition = panelComponents.indexOf(autoBraceOptions.head)
      panel.add(new JLabel(ScalaBundle.message("control.curly.braces.based.on.line.indents")), groupCaptionPosition)

      // add a left margin
      autoBraceOptions.foreach(_.setBorder(JBUI.Borders.emptyLeft(16)))
    }

    result
  }
}