plugins {
    kotlin("js")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":common"))
    implementation(kotlin("stdlib-js"))

    implementation("com.github.juggernaut0.kui:kui:0.11.0")
    implementation("dev.twarner.auth:auth-ui:3")

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
                useExperimentalAnnotation("kotlin.RequiresOptIn")
            }
        }
        binaries.executable()
    }
}
