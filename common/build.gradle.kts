import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    js {
        browser()
    }

    sourceSets {
        all {
            languageSettings {
                useExperimentalAnnotation("kotlin.RequiresOptIn")
            }
        }

        val multiplatformUtilsVersion = "0.6.3-graphql-SNAPSHOT"

        val commonMain by getting {
            dependencies {
                api("com.github.juggernaut0:multiplatform-utils:$multiplatformUtilsVersion")
                api("com.github.juggernaut0:multiplatform-utils-graphql:$multiplatformUtilsVersion")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")
            }
        }

        val jvmMain by getting {
            dependencies {
                api("com.github.juggernaut0:multiplatform-utils-ktor-jvm:$multiplatformUtilsVersion")
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
