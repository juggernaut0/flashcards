pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://juggernaut0.github.io/m2/repository")
    }

    plugins {
        id("dev.twarner.common") version "0.3.3"

        val kotlinVersion = "1.8.0"
        kotlin("js") version kotlinVersion
        kotlin("jvm") version kotlinVersion
        kotlin("kapt") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://juggernaut0.github.io/m2/repository")
    }

    versionCatalogs {
        create("libs") {
            from("dev.twarner:catalog:0.3.3")
            version("multiplatform-utils", "0.8.0-graphql-SNAPSHOT")
        }
    }
}

rootProject.name = "flashcards"
include("common", "dbmigrate", "service", "web")
