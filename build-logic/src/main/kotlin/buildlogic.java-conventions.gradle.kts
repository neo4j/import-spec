plugins {
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless")
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

group = "org.neo4j.importer"
version = "1.0.0-SNAPSHOT"

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        ktfmt().kotlinlangStyle()
        licenseHeaderFile(rootProject.file("license-header.txt"))
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktfmt().kotlinlangStyle()
    }
}
