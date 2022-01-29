val projectVersion: String by rootProject
version = projectVersion

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://juggernaut0.github.io/m2/repository")
    google()
}
