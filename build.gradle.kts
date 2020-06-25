import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm") version "1.3.70"
    maven
    id("org.jetbrains.dokka") version "0.10.1"
}

group = "com.github.aymanizz"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://dl.bintray.com/kotlin/dokka")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.ktor:ktor-server-core:1.3.1")
    testImplementation("io.ktor:ktor-server-test-host:1.3.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/docs"
    }
}
