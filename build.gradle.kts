import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("java")
    id("checkstyle")
}

group = "vad0"
version = "1.0-SNAPSHOT"

apply(plugin = "checkstyle")

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.typesafe:config:1.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("checkstyleAll") {
    group = "verification"
    dependsOn(tasks.withType<Checkstyle>())
}

tasks.withType<Checkstyle>().forEach { t ->
    t.reports.apply {
        xml.required.set(false)
        html.required.set(false)
    }
}

java {
    val javaVersion = JavaVersion.VERSION_17
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events("PASSED", "SKIPPED", "FAILED")
    }
}
