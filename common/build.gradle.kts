import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization").version("1.3.61")
}

kotlin {
    jvm()
    js()

    sourceSets {
        val multiplatformUtilsVersion = "0.2.0"
        val serializationVersion = "0.14.0"

        val commonMain by getting {
            dependencies {
                api("com.github.juggernaut0:multiplatform-utils:$multiplatformUtilsVersion")
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
            }
        }

        val jvmMain by getting {
            dependencies {
                api("com.github.juggernaut0:multiplatform-utils-jvm:$multiplatformUtilsVersion")
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
            }
        }

        val jsMain by getting {
            dependencies {
                api("com.github.juggernaut0:multiplatform-utils-js:$multiplatformUtilsVersion")
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
            }
        }
    }
}

tasks.withType<Kotlin2JsCompile> {
    kotlinOptions {
        moduleKind = "commonjs"
        sourceMap = true
        sourceMapEmbedSources = "always"
    }
}
