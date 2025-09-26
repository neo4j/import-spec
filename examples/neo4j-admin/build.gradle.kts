plugins { id("buildlogic.java-conventions") }

dependencies {
    testImplementation(project(":import-spec"))
    testImplementation(libs.org.assertj.assertj.core)
    testImplementation(libs.org.duckdb.duckdb.jdbc)
    testImplementation(libs.org.junit.jupiter.junit.jupiter)
    testImplementation(libs.org.neo4j.neo4j.cypher.dsl)
    testImplementation(libs.org.neo4j.driver.neo4j.java.driver)
    testImplementation(libs.org.slf4j.slf4j.nop)
    testImplementation(libs.org.testcontainers.junit.jupiter)
    testImplementation(libs.org.testcontainers.neo4j)
    testImplementation(libs.org.testcontainers.testcontainers)
}

description = "neo4j-admin-example"

java {
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
