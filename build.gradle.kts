import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    id("tasks.ts.modifier")
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
            implementation(libs.kotlinx.schema)
            implementation(libs.kotlinx.serializer.json)
            implementation(libs.kotlinx.yamlkt)
            implementation(libs.kaseChange)
        }
        jsMain.dependencies {
            implementation(libs.kotlin.js.plain.objects)
            implementation(libs.kotlin.wrappers.ts)
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
            freeCompilerArgs.add("-Xes-long-as-bigint")
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

tasks.withType<Kotlin2JsCompile>().configureEach {
    compilerOptions {
        target = "es2015"
    }
}

/*
    Kotlin/JS doesn't support TypeScript unions
    https://youtrack.jetbrains.com/issue/KT-55101/
    This script modifies the generated types and generates a string union given a basic enum.
    It's a somewhat brittle hack but the type safety is much preferred on the frontend.
    There's the potential to use a different library for TS generation in the future which does support this natively.
 */
tasks.register("generateTsUnions", TypeScriptModifierTask::class.java) {
    typescriptFile =
        layout.buildDirectory
            .dir("dist/js/productionLibrary/")
            .get()
            .file("graph-spec.d.mts")
            .asFile
}

tasks.named("jsBrowserProductionLibraryDistribution") {
    finalizedBy("generateTsUnions")
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
