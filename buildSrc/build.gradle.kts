plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.20")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.6.20")
    implementation("de.undercouch:gradle-download-task:5.0.4")
}
