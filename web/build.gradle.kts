import com.moowork.gradle.node.npm.NpxTask
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("js")
    id("kotlin-dce-js")
    id("com.github.node-gradle.node") version "2.2.1"
}

dependencies {
    implementation(project(":common"))
    implementation(kotlin("stdlib-js"))

    implementation("com.github.juggernaut0.kui:kui:0.10.0")
}

node {
    download = true
    version = "12.16.0"
}

tasks {
    runDceKotlin {
        keep("kotlin.defineModule")
    }

    withType<Kotlin2JsCompile> {
        kotlinOptions {
            moduleKind = "commonjs"
            sourceMap = true
            sourceMapEmbedSources = "always"
        }
    }

    val populateNodeModules by registering(Copy::class) {
        dependsOn(runDceKotlin)

        from(runDceKotlin.map { it.destinationDir })
        include("*.js", "*.js.map")
        into("$buildDir/node_modules")
    }

    val webpack by registering(NpxTask::class) {
        dependsOn(populateNodeModules, npmInstall)
        command = "webpack"
    }

    val webpackMin by registering(NpxTask::class) {
        dependsOn(populateNodeModules, npmInstall)
        command = "webpack"
        setArgs(listOf("--mode=production"))
    }
}
