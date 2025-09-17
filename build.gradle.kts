
plugins {
    kotlin("jvm") version "2.0.20"
    id("io.ktor.plugin") version "3.2.3"
}

application {
    mainClass.set("dev.rianniello.MainKt")
    applicationDefaultJvmArgs = listOf("-Duser.timezone=UTC")
}

repositories {
    mavenCentral()
}
dependencies {
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-jackson:2.3.12")
    implementation("com.nimbusds:nimbus-jose-jwt:9.41")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
}
