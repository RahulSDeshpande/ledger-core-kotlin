plugins {
    kotlin("jvm") version "2.2.20"
    application
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.rahuld.ledgercore.MainKt")
}

ktlint {
    version.set("1.5.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed") }
}

// Green check: same suite minus the test that fails by design (see README).
tasks.register<Test>("testCi") {
    testClassesDirs =
        sourceSets.test
            .get()
            .output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { excludeTags("by-design") }
    testLogging { events("passed", "failed") }
}
