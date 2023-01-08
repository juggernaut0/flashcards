import dev.twarner.gradle.DownloadFirefoxTask
import dev.twarner.gradle.SassTask

plugins {
    id("dev.twarner.common")
    kotlin("js")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":common"))
    implementation(kotlin("stdlib-js"))

    implementation(libs.kui)
    implementation(libs.twarner.auth.ui)

    implementation(npm("idb-keyval", "5.0.6"))

    testImplementation(kotlin("test-js"))
}

val firefoxVersion = "108.0.2"

kotlin {
    js {
        browser {
            testTask {
                environment("FIREFOX_BIN", "${gradle.gradleUserHomeDir}/firefox/$firefoxVersion/firefox/firefox")
                useKarma {
                    useFirefoxHeadless()
                }
            }
        }
        compilations.all {
            kotlinOptions {
                moduleKind = "commonjs"
                sourceMap = true
                sourceMapEmbedSources = "always"
            }
        }
        sourceSets.all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }
        binaries.executable()
    }
}

tasks {
    val assembleSassSrc by registering(Copy::class) {
        val processResources = named<Copy>("processResources")
        dependsOn(processResources, "jsJar")
        from(processResources.map { it.destinationDir })
        configurations["runtimeClasspath"].forEach {
            from(zipTree(it.absolutePath).matching { include("**/*.scss") })
        }
        includeEmptyDirs = false
        into(layout.buildDirectory.dir("sassSrc"))
    }

    val runSass by registering(SassTask::class) {
        dependsOn(assembleSassSrc)
        inputDir.set(layout.dir(assembleSassSrc.map { it.destinationDir }))
        version.set("1.49.0")
    }

    assemble {
        dependsOn(runSass)
    }

    val downloadFirefox by registering(DownloadFirefoxTask::class) {
        version.set(firefoxVersion)
    }

    named("browserTest") {
        dependsOn(downloadFirefox)
    }
}
