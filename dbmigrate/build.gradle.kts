import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile

plugins {
    kotlin("jvm")
    application
    id("com.bmuschko.docker-remote-api") version "7.1.0"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.postgresql:postgresql:42.3.1")
    implementation("org.flywaydb:flyway-core:7.15.0")
}

application {
    mainClassName = "flashcards.MigrateKt"
}

tasks {
    run.invoke {
        args = listOf("postgres://flashcards:flashcards@localhost:6432/flashcards")
    }
}
