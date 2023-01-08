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
    kotlin("plugin.serialization")
    id("dev.twarner.docker")
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
}
