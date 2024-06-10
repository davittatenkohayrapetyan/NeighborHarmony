plugins {
    kotlin("jvm") version "1.8.10"
    application
    id("org.beryx.jlink") version "2.24.4"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")
    implementation("io.github.microutils:kotlin-logging:2.1.21") // Correct artifact name
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation("javazoom:jlayer:1.0.1")
    implementation("com.mpatric:mp3agic:0.9.1") // Add mp3agic for MP3 metadata

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("MainKt") // Replace with your main class
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}

tasks.register<JavaExec>("runShadowJar") {
    group = "application"
    mainClass.set(application.mainClass.get())
    classpath = sourceSets.main.get().runtimeClasspath
}

jlink {
    options = listOf("--strip-debug", "--compress=2", "--no-header-files", "--no-man-pages")
    launcher {
        name = "NeighborHarmony"
    }
    jpackage {
        imageOptions = listOf(
            "--app-version", "1.0.0",
            "--vendor", "NeighborHarmony"
        )
        installerType = "exe"
        installerOptions = listOf(
            "--win-menu",
            "--win-shortcut",
            "--win-dir-chooser",
            "--win-menu-group", "NeighborHarmony"
        )
    }
}
