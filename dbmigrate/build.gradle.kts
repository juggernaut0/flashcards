plugins {
    id("kotlin-conventions")
    kotlin("jvm")
    application
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.postgresql:postgresql:42.3.6")
    implementation("org.flywaydb:flyway-core:8.5.11")
}

application {
    mainClass.set("flashcards.MigrateKt")
}

tasks {
    (run) {
        args = listOf("postgres://flashcards:flashcards@localhost:6432/flashcards")
    }
}
