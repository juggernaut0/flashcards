plugins {
    id("dev.twarner.kotlin-web")
    kotlin("plugin.serialization")
}

dependencies {
    jsMainImplementation(project(":common"))

    jsMainImplementation(libs.kui)
    jsMainImplementation(libs.twarner.auth.ui)

    jsMainImplementation(npm("idb-keyval", "5.0.6"))

    jsTestImplementation(kotlin("test"))
}

kotlin {
    js(IR) {
        sourceSets.all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }
    }
}
