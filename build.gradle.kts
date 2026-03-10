import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.js.plain.objects)
    id("maven-publish")
    alias(libs.plugins.axion.release)
}

group = "com.neo4j.importer.spec"

version = scmVersion.version

repositories { mavenCentral() }

kotlin {
    // Override target source sets for KMP
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serializer.json)
            implementation(libs.kotlinx.yamlkt)
        }
        jsMain.dependencies {
            implementation(libs.kotlin.js.plain.objects)
            implementation(libs.kotlin.wrappers.js)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
        testRuns.named("test") { executionTask.configure { useJUnitPlatform() } }
    }
    js(IR) {
        binaries.library()
        browser()
        compilerOptions {
            sourceMap = true
            moduleKind = JsModuleKind.MODULE_ES
            freeCompilerArgs.add("-opt-in=kotlin.js.ExperimentalWasmJsInterop")
        }
        generateTypeScriptDefinitions()
    }
    macosX64 { binaries.sharedLib() }
    macosArm64 { binaries.sharedLib() }
    linuxX64 { binaries.sharedLib() }
    linuxArm64 { binaries.sharedLib() }

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.js.ExperimentalJsExport")
        freeCompilerArgs.add("-opt-in=kotlin.js.ExperimentalJsStatic")
    }
}

tasks.named("jsBrowserProductionLibraryDistribution") {
    finalizedBy("generateTsUnions")
}

tasks.register("generateTsUnions") {
    doLast {
        val mtsFile = file("./build/dist/js/productionLibrary/graph-spec.d.mts")
        if (!mtsFile.exists()) {
            println("No definitions file found")
            return@doLast
        }
        var content = mtsFile.readText()
        val start = content.indexOf("export declare interface NodeConstraintJs extends ConstraintJs {")
        if (start == -1) {
            println("No node constraint interface found")
            return@doLast
        }
        content =
            content
                .replaceRange(
                    start - 1..start - 1,
                    "\nexport type ConstraintTypeJs = \"EXISTS\" | \"KEY\" | \"TYPE\" | \"UNIQUE\";\n"
                )
        val index = content.indexOf("label: string", start + 64)
        if (index == -1) {
            println("No node constraint label found")
            return@doLast
        }
        content =
            content.replaceRange(
                index..index + 13,
                "label: ConstraintTypeJs"
            )
        mtsFile.writeText(content)
    }
}

scmVersion {
    versionCreator("versionWithBranch")
    tag { prefix.set("graph-spec") }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name = "graph-spec"
                description = "Uniform Graph Specification Library for Neo4j"
                url = "https://github.com/neo4j/import-spec"
                inceptionYear = "2024"
                organization {
                    name = "Neo4j, Neo4j Sweden AB"
                    url = "https://neo4j.com"
                }
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "manual"
                    }
                }
                developers {
                    developer {
                        id = "team-connectors"
                        name = "Connectors Team"
                        organization = "Neo4j"
                        organizationUrl = "https://neo4j.com"
                    }
                    developer {
                        id = "team-data-importer"
                        name = "Data Importer Team"
                        organization = "Neo4j"
                        organizationUrl = "https://neo4j.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/neo4j/import-spec.git"
                    developerConnection = "scm:git:git@github.com:neo4j/import-spec.git"
                    url = "https://github.com/neo4j/import-spec"
                }
            }
        }
    }
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        ktlint().editorConfigOverride(
            mapOf("code_style" to "intellij_idea")
        )
        endWithNewline()
        licenseHeaderFile(rootProject.file("license-header.txt"))
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
        endWithNewline()
    }
    kotlin {
        target(
            project.fileTree("src/commonMain/kotlin"),
            project.fileTree("src/commonTest/kotlin"),
            project.fileTree("src/jsMain/kotlin")
        )
    }
}
