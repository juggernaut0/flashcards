import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

abstract class SassTask : DefaultTask() {
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val download = DownloadAction(project, this)
        download.src("https://github.com/sass/dart-sass/releases/download/1.35.1/dart-sass-1.35.1-linux-x64.tar.gz")
        download.dest("${project.buildDir}/sassTar.tar.gz")
        download.overwrite(false)
        download.execute()

        project.copy {
            from(project.tarTree("${project.buildDir}/sassTar.tar.gz"))
            into("${project.buildDir}/sass")
        }

        project.exec {
            executable("${project.buildDir}/sass/dart-sass/sass")
            args(inputFile.get(), outputFile.get())
        }
    }
}
