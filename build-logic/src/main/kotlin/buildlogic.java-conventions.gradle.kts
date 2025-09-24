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
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name.set(project.name)
            description.set("Uniform Import Specification Library for Neo4j")
            url.set("https://github.com/neo4j/import-spec")
            inceptionYear.set("2024")
            organization {
                name.set("Neo4j, Neo4j Sweden AB")
                url.set("https://neo4j.com")
            }
            licenses {
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("manual")
                }
            }
            developers {
                developer {
                    id.set("team-connectors")
                    name.set("Connectors Team")
                    organization.set("Neo4j")
                    organizationUrl.set("https://neo4j.com")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/neo4j/import-spec.git")
                developerConnection.set("scm:git:git@github.com:neo4j/import-spec.git")
                url.set("https://github.com/neo4j/import-spec")
            }
        }
    }

    repositories {
        maven {
            name = project.findProperty("repositoryId") as? String ?: "default"
            url = uri(project.findProperty("repositoryUrl") ?: "${layout.buildDirectory}/repo")
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        palantirJavaFormat()
    }
    kotlin {
        ktfmt().kotlinlangStyle()
        licenseHeaderFile(rootProject.file("license-header.txt"))
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktfmt().kotlinlangStyle()
    }
}
