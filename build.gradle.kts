import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.70"
    `maven-publish`
    id("org.jetbrains.dokka") version "0.10.1"
    id("com.diffplug.gradle.spotless") version "4.4.0"
}

group = "com.github.aymanizz"
version = "1.0.0"

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

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Creates a sources jar"
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
    dependsOn(tasks.classes)
}

tasks.getting(DokkaTask::class) {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
}

artifacts {
    archives(sourcesJar)
    archives(dokkaJar)
}

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
