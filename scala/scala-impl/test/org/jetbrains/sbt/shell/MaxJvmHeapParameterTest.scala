package org.jetbrains.sbt.shell

import java.io.File

import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import org.jetbrains.sbt.JvmMemorySize
import org.jetbrains.sbt.project.settings.SbtExecutionSettings

class MaxJvmHeapParameterTest extends TestCase {
  import org.junit.Assert.*

  val hiddenDefaultSize = JvmMemorySize.Megabytes(1500)
  val hiddenDefaultParam = "-Xmx" + hiddenDefaultSize
  val superShellDisabled = "-Dsbt.supershell=false"

  def buildParamSeq(userOpts: String*)(jvmOpts: String*): Seq[String] = {
    val workingDir = FileUtil.createTempDirectory("maxHeapJvmParamTest", getName, true)

    if (jvmOpts.nonEmpty) {
      val jvmOptsFile = new File(workingDir,".jvmopts")
      FileUtil.writeToFile(jvmOptsFile, jvmOpts.mkString("\n"))
    }

    val settings = new SbtExecutionSettings(null, null, userOpts, hiddenDefaultSize, null, null, null, null,
                                            false, false, false, false, false ,false)

    SbtProcessManager.buildVMParameters(settings, workingDir)
  }

  /*
    has userOpts xmx =>
       use xmx from userOpts
    has no userOpts xmx =>
       max(hidden default xmx, xms aus jvmopts
   */

  def testUserSettingsSmallerThanHiddenDefault(): Unit = {
    assertEquals(
      Seq(superShellDisabled,"-Xmx4g", "-Xms4g", "-Xmx1g"),
      buildParamSeq("-Xmx1g")("-Xmx4g", "-Xms4g")
    )
  }

  def testUserSettingsGreaterThanHiddenDefault(): Unit = {
    assertEquals(
      Seq(superShellDisabled,"-Xmx4g", "-Xms4g", "-Xmx2g"),
      buildParamSeq("-Xmx2g")("-Xmx4g", "-Xms4g")
    )
  }

  def testNoSettings(): Unit = {
    assertEquals(
      Seq(hiddenDefaultParam, superShellDisabled),
      buildParamSeq()()
    )
  }

  def testNoSettingsWithXmsSmallerThanDefaultParam(): Unit = {
    assertEquals(
      Seq(hiddenDefaultParam, superShellDisabled, "-Xms1g"),
      buildParamSeq("-Xms1g")()
    )
  }

  def testNoSettingsWithXmsGreaterThanDefaultParam(): Unit = {
    assertEquals(
      Seq(superShellDisabled, "-Xms2g"),
      buildParamSeq("-Xms2g")()
    )
  }
}
