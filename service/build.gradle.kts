import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import dev.twarner.gradle.SassTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("dev.twarner.common")
    kotlin("jvm")
    java
    application
    alias(libs.plugins.jooq)
    kotlin("kapt")
    alias(libs.plugins.docker.api)
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":dbmigrate"))

    implementation(kotlin("stdlib-jdk8"))

    implementation(platform(libs.ktor.bom))
    implementation(libs.bundles.ktor.server.jetty)
    implementation(libs.ktor.client.apache)

    implementation(libs.dagger)
    kapt(libs.dagger.compiler)

    implementation(libs.logback)

    jooqGenerator(libs.postgresql)
    implementation(libs.bundles.r2dbc.postgresql)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")

    implementation(libs.config4k)

    implementation(libs.twarner.auth.common)

    testImplementation(kotlin("test-junit"))
}

kotlin {
    sourceSets.all {
        languageSettings {
            optIn("kotlin.RequiresOptIn")
        }
    }
}

application {
    mainClass.set("flashcards.MainKt")
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
        kotlinOptions.jvmTarget = "17"
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
        include("*.js", "*.js.map")
    }

    val copyCss by registering(Copy::class) {
        val runSass = project(":web").tasks.named<SassTask>("runSass")
        dependsOn(runSass)
        from(runSass.flatMap { it.outputDir.dir("styles") })
        into("$buildDir/resources/main/static/css")
    }

    classes {
        dependsOn(copyWeb, copyCss)
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

        from("openjdk:17-slim")
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
