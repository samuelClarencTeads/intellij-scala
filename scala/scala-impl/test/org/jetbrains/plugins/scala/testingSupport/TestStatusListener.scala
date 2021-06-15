package org.jetbrains.plugins.scala.testingSupport

import com.intellij.execution.testframework.sm.runner.events.TestOutputEvent
import com.intellij.execution.testframework.sm.runner.{SMTRunnerEventsAdapter, SMTestProxy}
import com.intellij.openapi.util.Key
import com.intellij.util.containers.ContainerUtil

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

class TestStatusListener extends SMTRunnerEventsAdapter {

  private val _uncapturedOutput: mutable.Buffer[(String, String, Key[?])] =
    ContainerUtil.createConcurrentList[(String, String, Key[?])].asScala

  def uncapturedOutput: String = _uncapturedOutput
    .map { case (_, text, typ) => s"[$typ] $text" }
    .mkString

  override def onTestOutput(proxy: SMTestProxy, event: TestOutputEvent): Unit =
    super.onTestOutput(proxy, event)

  override def onUncapturedOutput(activeProxy: SMTestProxy, text: String, `type`: Key[?]): Unit = {
    val nodeName = activeProxy.getName
    _uncapturedOutput.append((nodeName, text, `type`))
  }
}