import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

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

tasks.withType<Kotlin2JsCompile>().configureEach {
    compilerOptions {
        target = "es2015"
    }
}

/*
    Kotlin/JS doesn't support TypeScript unions
    This script modifies the generated types and generates a string union given a basic enum.
    It's a brittle hack but the type safety is much preferred on the frontend.
    There's the potential to use a different library for TS generation in the future which does support this natively.
 */
tasks.register("generateTsUnions") {
    doLast {
        val mtsFile = file("./build/dist/js/productionLibrary/graph-spec.d.mts")
        require(mtsFile.exists()) { "Typescript file missing." }
        var content = mtsFile.readText()
        content = generateUnion(content, "ConstraintType", "ConstraintTypeJs")
        content = setUnionType(content, "NodeConstraintJs", "type", "ConstraintTypeJs")
        content = setUnionType(content, "RelationshipConstraintJs", "type", "ConstraintTypeJs")
        content = generateUnion(content, "IndexType", "IndexTypeJs")
        content = setUnionType(content, "NodeIndexJs", "type", "IndexTypeJs")
        content = setUnionType(content, "RelationshipIndexJs", "type", "IndexTypeJs")
        content = generateUnion(content, "MappingMode", "MappingModeJs")
        content = setUnionType(content, "NodeMappingJs", "mode", "MappingModeJs")
        mtsFile.writeText(content)
    }
}

private fun generateUnion(
    file: String,
    enum: String,
    union: String
): String {
    if (file.contains("export type $union")) {
        return file
    }
    val index = file.indexOf("export declare abstract class $enum ")
    require(index != -1) { "No class found $enum" }
    val final = file.indexOf("}\n", index)
    require(final != -1) { "No enum found for class $enum" }
    val text = file.substring(index, final)

    val start = text.lastIndexOf("get name(): ")
    require(start != -1) { "No enum found for class $enum" }
    val end = text.indexOf(";", start + 12)
    require(end != -1) { "No enum found for class $enum" }
    val types = text.substring(start + 12, end)
    return file.replaceRange(index..index, "export type $union = $types\ne")
}

private fun setUnionType(
    file: String,
    parent: String,
    param: String,
    enum: String
): String {
    val index = file.indexOf("export declare interface $parent ")
    require(index != -1) { "Unable to find parent class $parent" }
    val start = file.indexOf("$param: string;")
    require(start != -1) { "Unable to find string param $parent" }
    return file.replaceRange(start..start + 8 + param.length, "$param: $enum;")
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
