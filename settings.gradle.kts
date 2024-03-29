pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven("https://juggernaut0.github.io/m2/repository")
    }

    plugins {
        val kotlinVersion = "1.9.23"
        kotlin("js") version kotlinVersion
        kotlin("jvm") version kotlinVersion
        kotlin("kapt") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
    }
}

plugins {
    id("dev.twarner.settings") version "1.1.0-SNAPSHOT"
}

dependencyResolutionManagement {
    versionCatalogs {
        named("libs") {
            version("multiplatformUtils", "0.11.0-SNAPSHOT")
        }
    }
}

rootProject.name = "flashcards"
include("common", "dbmigrate", "service", "web")
