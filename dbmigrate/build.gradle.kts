import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile

plugins {
    kotlin("jvm")
    application
    id("com.bmuschko.docker-remote-api") version "6.7.0"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.postgresql:postgresql:42.2.23")
    implementation("org.flywaydb:flyway-core:6.5.7")
}

application {
    mainClassName = "flashcards.MigrateKt"
}

tasks {
    run.invoke {
        args = listOf("postgres://flashcards:flashcards@localhost:6432/flashcards")
    }
}
