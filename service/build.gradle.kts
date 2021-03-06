import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    java
    application
    id("nu.studer.jooq").version("5.2.1")
    kotlin("kapt")
    id("com.bmuschko.docker-remote-api") version "6.1.3"
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":dbmigrate"))

    implementation(kotlin("stdlib-jdk8"))

    val ktorVersion = "1.6.0"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-jetty:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

    val daggerVersion = "2.36"
    implementation("com.google.dagger:dagger:$daggerVersion")
    kapt("com.google.dagger:dagger-compiler:$daggerVersion")

    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation("org.postgresql:postgresql:42.2.5")
    implementation("org.jooq:jooq:3.14.11")
    jooqGenerator("org.postgresql:postgresql:42.2.5")
    implementation("com.zaxxer:HikariCP:3.2.0")

    implementation("io.github.config4k:config4k:0.4.2")

    implementation("dev.twarner.auth:auth-common:3")

    testImplementation(kotlin("test-junit"))
}

kotlin {
    sourceSets.all {
        languageSettings {
            useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
    }
}

application {
    mainClassName = "flashcards.MainKt"
}

jooq {
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(true)
            jooqConfiguration.apply {
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:6432/flashcards"
                    user = "flashcards"
                    password = "flashcards"
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    strategy.apply {
                        name = "org.jooq.codegen.DefaultGeneratorStrategy"
                    }
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        includes = ".*"
                        excludes = "flyway_schema_history"
                    }
                    generate.apply {
                        isRelations = true
                        isDeprecated = false
                        isRecords = true
                        isFluentSetters = false
                    }
                    target.apply {
                        packageName = "flashcards.db.jooq"
                        directory = "build/generated/source/jooq/main"
                    }
                }
            }
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    val copyWeb by registering(Copy::class) {
        if (version.toString().endsWith("SNAPSHOT")) {
            dependsOn(":web:browserDevelopmentWebpack")
        } else {
            dependsOn(":web:browserProductionWebpack")
        }
        group = "build"
        from("${project(":web").buildDir}/distributions")
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
