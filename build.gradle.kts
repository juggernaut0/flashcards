plugins {
    kotlin("jvm") version "1.5.10" apply false
    kotlin("plugin.serialization") version "1.5.10" apply false
}

subprojects {
    version = "22"

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://juggernaut0.github.io/m2/repository")
        google()
    }
}
