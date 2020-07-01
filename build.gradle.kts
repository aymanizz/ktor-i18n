import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.70"
    maven
    id("org.jetbrains.dokka") version "0.10.1"
    id("com.diffplug.gradle.spotless") version "4.4.0"
}

group = "com.github.aymanizz"

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://dl.bintray.com/kotlin/dokka")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-core:1.3.1")
    testImplementation("io.ktor:ktor-server-test-host:1.3.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += listOf("-Werror")
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    format("misc") {
        target("*.gradle", "**/*.md", "**/.gitignore")

        trimTrailingWhitespace()
        indentWithSpaces(2)
        endWithNewline()
    }

    val ktlintUserData = mapOf(
        "indent_size" to "4", "continuation_indent_size" to "4", "enableExperimentalRules" to "true"
    )

    kotlin {
        ktlint("0.37.2").apply {
            userData(ktlintUserData)
        }
    }
    kotlinGradle {
        ktlint("0.37.2").apply {
            userData(ktlintUserData)
        }
    }
}

tasks {
    getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/docs"
    }
}
