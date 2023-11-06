import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("java")
    id("checkstyle")
    id("maven-publish")
}

apply(plugin = "checkstyle")
apply(plugin = "maven-publish")

group = "vad0"
version = "1.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.typesafe:config:1.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
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

publishing {
    publications {
        // This mavenJava can be filled in randomly, it's just a task name
        // MavenPublication must have, this is the task class to call
        create<MavenPublication>("maven") {
            // The header here is the artifacts configuration information, do not fill in the default
            from(components["java"])
        }
    }
}
