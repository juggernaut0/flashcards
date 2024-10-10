pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven("https://juggernaut0.github.io/m2/repository")
    }

    plugins {
        id("dev.twarner.common") version "0.3.4"

        val kotlinVersion = "2.0.21"
        kotlin("js") version kotlinVersion
        kotlin("jvm") version kotlinVersion
        kotlin("kapt") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven("https://juggernaut0.github.io/m2/repository")
    }

    versionCatalogs {
        create("libs") {
            from("dev.twarner:catalog:0.3.4")
            version("multiplatform-utils", "0.8.0-graphql-SNAPSHOT")
        }
    }
}

rootProject.name = "flashcards"
include("common", "dbmigrate", "service", "web")
