plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.0")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.7.0")
    implementation("de.undercouch:gradle-download-task:5.1.0")
}
