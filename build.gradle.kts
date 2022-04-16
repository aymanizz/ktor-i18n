import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    `maven-publish`
    id("com.diffplug.spotless") version "6.4.2"
}

group = "com.github.aymanizz"
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-core:2.0.0")
    testImplementation("io.ktor:ktor-server-test-host:2.0.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.20")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += listOf("-Werror")
}

tasks.test {
    useJUnitPlatform()
}

java.withSourcesJar()

spotless {
    format("misc") {
        target("*.gradle", "**/*.md", "**/.gitignore")

        trimTrailingWhitespace()
        indentWithSpaces(2)
        endWithNewline()
    }

    val ktlintVersion = "0.37.2"
    val ktlintUserData = mapOf(
        "indent_size" to "4", "continuation_indent_size" to "4", "enableExperimentalRules" to "true"
    )

    kotlin {
        ktlint(ktlintVersion).apply {
            userData(ktlintUserData)
        }
    }
    kotlinGradle {
        ktlint(ktlintVersion).apply {
            userData(ktlintUserData)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("ktor-i18n") {
            from(components["java"])
        }
    }
}
