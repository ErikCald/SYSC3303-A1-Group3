
plugins {
    // Apply the application plugin to add support for an application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // Define the main class for the application.
    mainClass = "sysc3303.a1.group3.Main"
}

tasks.test {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
    maxParallelForks = 1
}
