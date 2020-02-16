import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    java
    application
    id("nu.studer.jooq").version("4.1")
    kotlin("kapt")
    id("com.bmuschko.docker-remote-api") version "6.1.3"
}

dependencies {
    implementation(project(":common"))

    implementation(kotlin("stdlib-jdk8"))

    implementation("io.javalin:javalin:3.7.0")

    val daggerVersion = "2.25.4"
    implementation("com.google.dagger:dagger:$daggerVersion")
    kapt("com.google.dagger:dagger-compiler:$daggerVersion")

    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation("org.postgresql:postgresql:42.2.5")
    implementation("org.jooq:jooq:3.12.3")
    jooqRuntime("org.postgresql:postgresql:42.2.5")
    implementation("com.zaxxer:HikariCP:3.2.0")

    implementation("io.github.config4k:config4k:0.4.1")

    implementation("dev.twarner.auth:auth-common:1")

    testImplementation(kotlin("test-junit"))
}

application {
    mainClassName = "flashcards.MainKt"
}

apply {
    from("jooq.gradle")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        dependsOn("generatePostgresJooqSchemaSource")
    }

    val copyWeb by registering(Copy::class) {
        if (version.toString().endsWith("SNAPSHOT")) {
            dependsOn(":web:webpack")
        } else {
            dependsOn(":web:webpackMin")
        }
        group = "build"
        from("${project(":web").buildDir}/webpack")
        into("$buildDir/resources/main/static/js")
    }

    classes {
        dependsOn(copyWeb)
    }

    run.invoke {
        systemProperty("config.file", "local.conf")
    }

    val copyDist by registering(Copy::class) {
        dependsOn(distTar)
        from(distTar.flatMap { it.archiveFile })
        into("$buildDir/docker")
    }

    val dockerfile by registering(Dockerfile::class) {
        dependsOn(copyDist)

        from("openjdk:11-jre-slim")
        addFile(distTar.flatMap { it.archiveFileName }.map { Dockerfile.File(it, "/app/") })
        defaultCommand(distTar.flatMap { it.archiveFile }.map { it.asFile.nameWithoutExtension }.map { listOf("/app/$it/bin/${project.name}") })
    }

    val dockerBuild by registering(DockerBuildImage::class) {
        dependsOn(dockerfile)

        if (version.toString().endsWith("SNAPSHOT")) {
            images.add("${rootProject.name}:SNAPSHOT")
        } else {
            images.add("juggernaut0/${rootProject.name}:$version")
        }
    }
}
