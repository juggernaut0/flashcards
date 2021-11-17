plugins {
    kotlin("jvm") version "1.6.0" apply false
    kotlin("plugin.serialization") version "1.6.0" apply false
}

subprojects {
    version = "31"

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://juggernaut0.github.io/m2/repository")
        google()
    }
}
