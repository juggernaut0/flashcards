plugins {
    kotlin("js")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":common"))
    implementation(kotlin("stdlib-js"))

    implementation("com.github.juggernaut0.kui:kui:0.14.0")
    implementation("dev.twarner.auth:auth-ui:7")

    implementation(npm("idb-keyval", "5.0.6"))

    testImplementation(kotlin("test-js"))
}

kotlin {
    js {
        browser {
            testTask {
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
    }

    assemble {
        dependsOn(runSass)
    }
}
