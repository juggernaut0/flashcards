import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class DownloadFirefoxTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @TaskAction
    fun run() {
        val version = version.get()
        val gradleHome = project.gradle.gradleUserHomeDir
        val destDir = "$gradleHome/firefox/$version"
        val downloadDest = "$destDir/firefox.tar.bz2"
        val unpackDest = destDir

        DownloadAction(project, this).apply {
            src("https://download-installer.cdn.mozilla.net/pub/firefox/releases/$version/linux-x86_64/en-US/firefox-$version.tar.bz2")
            dest(downloadDest)
            overwrite(false)
        }.execute().get()

        // skip unpack if it exists
        if (File(unpackDest, "firefox").exists()) return

        project.copy {
            from(project.tarTree(downloadDest))
            into(unpackDest)
        }
    }
}
