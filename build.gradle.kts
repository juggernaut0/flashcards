plugins {
    kotlin("jvm") version "1.3.61" apply false
}

subprojects {
    version = "1-SNAPSHOT"

    repositories {
        mavenLocal()
        maven("https://kotlin.bintray.com/kotlinx")
        mavenCentral()
        jcenter()
        maven("https://juggernaut0.github.io/m2/repository")
    }
}
