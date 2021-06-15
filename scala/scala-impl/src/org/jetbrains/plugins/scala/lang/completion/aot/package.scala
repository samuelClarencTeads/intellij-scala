package org.jetbrains.plugins.scala.lang
package completion

import com.intellij.codeInsight.completion.{InsertionContext, InsertHandler as IJInsertHandler}
import com.intellij.codeInsight.lookup.{LookupElement, LookupElementDecorator, LookupElementPresentation, LookupElementRenderer as IJLookupElementRenderer}

package object aot {

  private[aot] val Delimiter = ": "

  private[aot] type Decorator = LookupElementDecorator[LookupElement]

  private[aot] class InsertHandler(itemText: String) extends IJInsertHandler[Decorator] {

    override final def handleInsert(context: InsertionContext, decorator: Decorator): Unit =
      handleInsert(decorator)(context)

    protected def handleInsert(decorator: Decorator)
                              (implicit context: InsertionContext): Unit = {
      handleReplace(context)
      decorator.getDelegate.handleInsert(context)
    }

    protected def handleReplace(implicit context: InsertionContext): Unit = {
      context.getDocument.replaceString(
        context.getStartOffset,
        context.getTailOffset,
        itemText
      )
      context.commitDocument()
    }
  }

  private[aot] class LookupElementRenderer(itemText: String) extends IJLookupElementRenderer[Decorator] {

    override def renderElement(decorator: Decorator,
                               presentation: LookupElementPresentation): Unit = {
      decorator.getDelegate.renderElement(presentation)

      presentation.setItemText(itemText)
      presentation.setTypeText(null)
    }
  }

}
