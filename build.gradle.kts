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
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
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
