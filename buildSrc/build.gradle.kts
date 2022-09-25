plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.7.10")
    implementation("de.undercouch:gradle-download-task:5.1.1")
}
