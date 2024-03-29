plugins {
    id("dev.twarner.kotlin-service")
    kotlin("kapt")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":dbmigrate"))
    webResource(project(":web"))

    implementation(kotlin("stdlib-jdk8"))

    implementation(platform(libs.ktor.bom))
    implementation(libs.bundles.ktor.server.jetty)
    implementation(libs.ktor.client.apache)

    implementation(libs.dagger)
    kapt(libs.dagger.compiler)

    implementation(libs.logback)

    implementation(libs.bundles.r2dbc.postgresql)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")

    implementation(libs.config4k)

    implementation(libs.twarner.auth.plugins.ktor)

    testImplementation(kotlin("test-junit"))
}

kotlin {
    sourceSets.all {
        languageSettings {
            optIn("kotlin.RequiresOptIn")
        }
    }
}

application {
    mainClass.set("flashcards.MainKt")
}

tasks {
    run.invoke {
        systemProperty("config.file", "local.conf")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
