package org.jetbrains.plugins.scala.actions

import java.util
import java.util.Collections

import com.intellij.openapi.actionSystem.{ActionPromoter, AnAction, DataContext}

/**
  * User: Dmitry.Naydanov
  * Date: 28.02.17.
  */
abstract class SingleActionPromoterBase extends ActionPromoter {
  def shouldPromote(anAction: AnAction, context: DataContext): Boolean
  
  override def promote(actions: util.List[? <: AnAction], context: DataContext): util.List[AnAction] = {
    val it = actions.iterator()

    while (it.hasNext) {
      val a = it.next()
      if (shouldPromote(a, context)) return util.Arrays.asList(a)
    }

    Collections.emptyList()
  }
}
