import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    id("dev.twarner.common")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }

        val commonMain by getting {
            dependencies {
                api("com.github.juggernaut0:multiplatform-utils-graphql:${libs.versions.multiplatformUtils.get()}")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            }
        }

        val jvmMain by getting {
            dependencies {
                api(libs.multiplatformUtils.ktor)
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
