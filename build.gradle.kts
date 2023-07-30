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
