package org.jetbrains.plugins.scala.components

import com.intellij.ide.plugins.*
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.application.{ApplicationInfo, ApplicationManager, PermanentInstallationID}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.updateSettings.impl.*
import com.intellij.openapi.util.{BuildNumber, JDOMUtil, SystemInfo}
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.HttpRequests.Request
import org.jdom.JDOMException
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings.pluginBranch
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings.pluginBranch.*
import org.jetbrains.plugins.scala.util.{ScalaNotificationGroups, UnloadAwareDisposable}
import org.jetbrains.plugins.scala.{ScalaBundle, ScalaFileType, extensions}

import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.swing.event.HyperlinkEvent
import scala.xml.XML

class InvalidRepoException(what: String) extends Exception(what)

//noinspection UnstableApiUsage,ApiStatus
object ScalaPluginUpdater {
  private val LOG = Logger.getInstance(getClass)
  private val scalaPluginId = "1347"
  private val baseUrl: String = "https://plugins.jetbrains.com/plugins/%s/" + scalaPluginId
  private var doneUpdating = false
  private def pluginDescriptor: IdeaPluginDescriptorImpl = ScalaPluginVersionVerifier.getPluginDescriptor

  // *_OLD versions are for legacy repository format.
  // Need to keep track of them to upgrade to new repository format automatically
  // Remove eventually, when no significant amount of users will have older ones.
  private val CASSIOPEIA_OLD    = "cassiopeia_old"
  private val FOURTEEN_ONE_OLD  = "14.1_old"
  private val FOURTEEN_ONE      = "14.1"

  private val knownVersions = Map(
    CASSIOPEIA_OLD   -> Map(
      Release -> "DUMMY",
      EAP     -> "https://www.jetbrains.com/idea/plugins/scala-eap-cassiopeia.xml",
      Nightly -> "https://www.jetbrains.com/idea/plugins/scala-nightly-cassiopeia.xml"
    ),
    FOURTEEN_ONE_OLD -> Map(
      Release -> "DUMMY",
      EAP     -> "https://www.jetbrains.com/idea/plugins/scala-eap-14.1.xml",
      Nightly -> ""
    ),
    FOURTEEN_ONE -> Map(
      Release -> "DUMMY",
      EAP     -> baseUrl.format("eap"),
      Nightly -> baseUrl.format("nightly")
    )
  )

  private val currentVersion: String = FOURTEEN_ONE

  private def currentRepo: Map[pluginBranch, String] = knownVersions(currentVersion)

  private val updGroupId = "Scala Plugin Update"
  private val title = updGroupId
  private def GROUP = ScalaNotificationGroups.stickyBalloonGroup

  // save plugin version before patching to restore it when switching back
  private var savedPluginVersion = ""

  private val updateListener = new DocumentListener {
    override def documentChanged(e: DocumentEvent): Unit = {
      val file = FileDocumentManager.getInstance().getFile(e.getDocument)
      if (file != null && file.getFileType == ScalaFileType.INSTANCE)
        scheduleUpdate()
    }
  }

  @throws(classOf[InvalidRepoException])
  def doUpdatePluginHosts(branch: ScalaApplicationSettings.pluginBranch, descriptor: IdeaPluginDescriptorImpl): AnyVal = {
    if(currentRepo(branch).isEmpty)
      throw new InvalidRepoException(s"Branch $branch is unavailable for IDEA version $currentVersion")

    // update hack - set plugin version to 0 when downgrading
    // also unpatch it back if user changed mind about downgrading
    if (getScalaPluginBranch.compareTo(branch) > 0) {
      savedPluginVersion = descriptor.getVersion
      patchPluginVersion("0.0.0", descriptor)
    } else if (savedPluginVersion.nonEmpty) {
      patchPluginVersion(savedPluginVersion, descriptor)
      savedPluginVersion = ""
    }

    val updateSettings = UpdateSettings.getInstance()
    updateSettings.getStoredPluginHosts.remove(currentRepo(EAP))
    updateSettings.getStoredPluginHosts.remove(currentRepo(Nightly))

    branch match {
      case Release => // leave default plugin repository
      case EAP     => updateSettings.getStoredPluginHosts.add(currentRepo(EAP))
      case Nightly => updateSettings.getStoredPluginHosts.add(currentRepo(Nightly))
    }
  }

  @throws(classOf[InvalidRepoException])
  def doUpdatePluginHostsAndCheck(branch: ScalaApplicationSettings.pluginBranch): Any = {
    doUpdatePluginHosts(branch, pluginDescriptor)
    if(UpdateSettings.getInstance().isCheckNeeded) {
      UpdateChecker.updateAndShowResult()
        .doWhenDone(() => postCheckIdeaCompatibility(branch))
    }
  }

  def getScalaPluginBranch: ScalaApplicationSettings.pluginBranch = {
    if (ScalaPluginUpdater.pluginIsEap) EAP
    else if (ScalaPluginUpdater.pluginIsNightly) Nightly
    else Release
  }

  def pluginIsEap: Boolean = {
    val updateSettings = UpdateSettings.getInstance()
    updateSettings.getStoredPluginHosts.contains(currentRepo(EAP))
  }

  def pluginIsNightly: Boolean = {
    val updateSettings = UpdateSettings.getInstance()
    updateSettings.getStoredPluginHosts.contains(currentRepo(Nightly))
  }

  def pluginIsRelease: Boolean = !pluginIsEap && !pluginIsNightly

  def upgradeRepo(): Unit = {
    val updateSettings = UpdateSettings.getInstance()
    for {
      case (version, repo) <- knownVersions
      if version != currentVersion
    } {
      if (updateSettings.getStoredPluginHosts.contains(repo(EAP))) {
        updateSettings.getStoredPluginHosts.remove(repo(EAP))
        if (currentRepo(EAP).nonEmpty)
          updateSettings.getStoredPluginHosts.add(currentRepo(EAP))
      }
      if (updateSettings.getStoredPluginHosts.contains(repo(Nightly))) {
        updateSettings.getStoredPluginHosts.remove(repo(Nightly))
        if (currentRepo(Nightly).nonEmpty)
          updateSettings.getStoredPluginHosts.add(currentRepo(Nightly))
        else if (currentRepo(EAP).nonEmpty)
          updateSettings.getStoredPluginHosts.add(currentRepo(EAP))
      }
    }
  }

  def postCheckIdeaCompatibility(branch: ScalaApplicationSettings.pluginBranch): Unit = {
    val infoImpl = ApplicationInfo.getInstance().asInstanceOf[ApplicationInfoImpl]
    val localBuildNumber = infoImpl.getBuild
    val url = branch match {
      case Release => None
      case EAP     => Some(currentRepo(EAP))
      case Nightly => Some(currentRepo(Nightly))
    }

    url.foreach(u => extensions.executeOnPooledThread {
      try {
        val factory = javax.xml.parsers.SAXParserFactory.newInstance()
        // disable DTD validation
        factory.setValidating(false)
        factory.setFeature("http://xml.org/sax/features/validation", false)
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        val resp = XML.withSAXParser(factory.newSAXParser).load(u)
        val text = ((resp \\ "idea-plugin").head \ "idea-version" \ "@since-build").text
        val remoteBuildNumber = BuildNumber.fromString(text)
        if (localBuildNumber.compareTo(remoteBuildNumber) < 0)
          suggestIdeaUpdate(branch.toString, text)
      }
      catch {
        case e: Throwable => LOG.warn("Failed to check plugin compatibility", e)
      }
    })
  }

  def postCheckIdeaCompatibility(): Unit = postCheckIdeaCompatibility(getScalaPluginBranch)

  private def suggestIdeaUpdate(branch: String, suggestedVersion: String): Unit = {
    val infoImpl = ApplicationInfo.getInstance().asInstanceOf[ApplicationInfoImpl]
    val appSettings = ScalaApplicationSettings.getInstance()

    if (!appSettings.ASK_PLATFORM_UPDATE)
      return

    def createPlatformChannelSwitchPopup(): Notification = {
      val switchIDEAToEAPQuestion = ScalaBundle.message("switch.idea.to.eap.question", branch)
      val yes = ScalaBundle.message("switch.yes")
      val notNow = ScalaBundle.message("switch.not.now")
      val ignore = ScalaBundle.message("switch.ignore.this.update")
      val links =
        s"""<p/><a href="Yes">$yes</a>""" +
          s"""<p/><a href="No">$notNow</a>""" +
          s"""<p/><a href="Ignore">$ignore</a>"""
      val message = switchIDEAToEAPQuestion + links
      GROUP.createNotification(
        ScalaBundle.message("scala.plugin.update.failed"),
        message,
        NotificationType.WARNING
      ).setListener(
        (notification: Notification, event: HyperlinkEvent) => {
          notification.expire()
          event.getDescription match {
            case "No"     => // do nothing, will ask next time
            case "Yes"    => UpdateSettings.getInstance().setSelectedChannelStatus(ChannelStatus.EAP)
            case "Ignore" => appSettings.ASK_PLATFORM_UPDATE = false
          }
        }
      )
    }

    def createPlatformUpdateSuggestPopup(): Notification = {
      GROUP.createNotification(
        ScalaBundle.message("idea.is.outdated.please.update", branch, suggestedVersion),
        NotificationType.WARNING)
    }

    def getPlatformUpdateResult: Option[PlatformUpdates] = {
      val url = ApplicationInfoEx.getInstanceEx.getUpdateUrls.getCheckingUrl

      val info: Option[Product] = HttpRequests.request(url).connect { request =>
        val productCode = ApplicationInfo.getInstance().getBuild.getProductCode
        try   { Option(UpdateData.parseUpdateData(JDOMUtil.load(request.getInputStream), productCode)) }
        catch { case e: JDOMException => LOG.info(e); None }
      }

      info.map(new UpdateStrategy(infoImpl.getBuild, _, UpdateSettings.getInstance()).checkForUpdates())
    }

    def isUpToDatePlatform(result: PlatformUpdates): Boolean =
      result match {
        case loaded: PlatformUpdates.Loaded =>
          loaded.getNewBuild.getNumber.compareTo(infoImpl.getBuild) <= 0
        case _ => true
      }

    val notification = getPlatformUpdateResult match {
      case Some(result) if isUpToDatePlatform(result) && !infoImpl.isEAP => // platform is up to date - suggest eap
        Some(createPlatformChannelSwitchPopup())
      case Some(_: PlatformUpdates.Loaded) =>
        Some(createPlatformUpdateSuggestPopup())
      case _ => None
    }

    notification.foreach(Notifications.Bus.notify)
  }

  private def scheduleUpdate(): Unit = {
    val key = "scala.last.updated"
    val lastUpdateTime = PropertiesComponent.getInstance().getLong(key , 0)
    EditorFactory.getInstance().getEventMulticaster.removeDocumentListener(updateListener)
    if (lastUpdateTime == 0L || System.currentTimeMillis() - lastUpdateTime > TimeUnit.DAYS.toMillis(1)) {
      ApplicationManager.getApplication.executeOnPooledThread(new Runnable {
        override def run(): Unit = {
          val buildNumber = ApplicationInfo.getInstance().getBuild.asString()
          val pluginVersion = pluginDescriptor.getVersion
          val os = URLEncoder.encode(SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION, CharsetToolkit.UTF8)
          val uid: String = PermanentInstallationID.get()
          val url = s"https://plugins.jetbrains.com/plugins/list?pluginId=$scalaPluginId&build=$buildNumber&pluginVersion=$pluginVersion&os=$os&uuid=$uid"
          PropertiesComponent.getInstance().setValue(key, System.currentTimeMillis().toString)
          doneUpdating = true
          try {
            HttpRequests.request(url).connect((request: Request) => JDOMUtil.load(request.getReader()))
          } catch {
            case e: Throwable => LOG.warn(e)
          }
        }
      })
    }
  }

  def setupReporter(): Unit = {
    if (ApplicationManager.getApplication.isUnitTestMode) return
    EditorFactory.getInstance().getEventMulticaster.addDocumentListener(updateListener, UnloadAwareDisposable.scalaPluginDisposable)
  }

  // this hack uses reflection to downgrade plugin version
  def patchPluginVersion(newVersion: String, descriptor: IdeaPluginDescriptorImpl): Unit = {
    val versionField = classOf[IdeaPluginDescriptorImpl].getDeclaredField("version")
    versionField.setAccessible(true)
    versionField.set(descriptor, newVersion)
    versionField.setAccessible(false)
  }

  def askUpdatePluginBranchIfNeeded(): Unit = {
    val infoImpl = ApplicationInfo.getInstance().asInstanceOf[ApplicationInfoImpl]
    val applicationSettings = ScalaApplicationSettings.getInstance()
    if (infoImpl.isEAP
      && applicationSettings.ASK_USE_LATEST_PLUGIN_BUILDS
      && ScalaPluginUpdater.pluginIsRelease) {
      val selectUpdateChannel = ScalaBundle.message("please.select.scala.plugin.update.channel")
      val stableReleases = ScalaBundle.message("channel.stable.releases")
      val eap = ScalaBundle.message("channel.early.access.program")
      val nightlyBuilds = ScalaBundle.message("channel.nightly.builds")
      val links =
        s"""<a href="Release">$stableReleases</a> | <a href="EAP">$eap</a> | <a href="Nightly">$nightlyBuilds</a>"""
      val message = selectUpdateChannel + "<p/>" + links

      val listener = new NotificationListener {
        override def hyperlinkUpdate(notification: Notification, event: HyperlinkEvent): Unit = {
          notification.expire()
          applicationSettings.ASK_USE_LATEST_PLUGIN_BUILDS = false
          event.getDescription match {
            case "Release" => doUpdatePluginHostsAndCheck(Release)
            case "EAP" => doUpdatePluginHostsAndCheck(EAP)
            case "Nightly" => doUpdatePluginHostsAndCheck(Nightly)
            case _ => applicationSettings.ASK_USE_LATEST_PLUGIN_BUILDS = true
          }
        }
      }

      val notification = GROUP.createNotification(title, message, NotificationType.INFORMATION).setListener(listener)
      val project = ProjectManager.getInstance().getOpenProjects.headOption.orNull
      Notifications.Bus.notify(notification, project)
    }
  }

}
