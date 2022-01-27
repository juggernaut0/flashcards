plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.postgresql:postgresql:42.3.1")
    implementation("org.flywaydb:flyway-core:8.4.2")
}

application {
    mainClass.set("flashcards.MigrateKt")
}

tasks {
    (run) {
        args = listOf("postgres://flashcards:flashcards@localhost:6432/flashcards")
    }
}
