plugins {
    id("dev.twarner.common")
    kotlin("jvm")
    application
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation(libs.postgresql)
    implementation(libs.flyway.core)
}

application {
    mainClass.set("flashcards.MigrateKt")
}

tasks {
    (run) {
        args = listOf("postgres://flashcards:flashcards@localhost:6432/flashcards")
    }
}
