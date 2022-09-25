import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

abstract class SassTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val version: Property<String>

    init {
        version.convention("1.38.0")
        outputDir.convention(project.layout.buildDirectory.dir("sass"))
    }

    @TaskAction
    fun run() {
        val version = version.get()
        val gradleHome = project.gradle.gradleUserHomeDir
        val sassDir = "$gradleHome/sass/$version"
        val downloadDest = "$sassDir/sass.tar.gz"
        val unpackDest = "$sassDir/sass"

        val download = DownloadAction(project, this)
        download.src("https://github.com/sass/dart-sass/releases/download/$version/dart-sass-$version-linux-x64.tar.gz")
        download.dest(downloadDest)
        download.overwrite(false)
        download.execute().get()

        project.copy {
            from(project.tarTree(downloadDest))
            into(unpackDest)
        }

        project.exec {
            executable("$unpackDest/dart-sass/sass")
            args("${inputDir.get()}:${outputDir.get()}")
        }
    }
}
