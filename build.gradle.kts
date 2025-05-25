plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "me.uippao"
version = "1.0.0"

repositories {
    mavenCentral()
}
application {
    mainClass.set("me.uippao.ifetch.MainKt")
}

dependencies {
    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Ktor (HTTP Server)
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-core:2.3.7")

    // YAML
    implementation("com.charleskorn.kaml:kaml:0.56.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")

    // XML
    implementation("io.github.pdvrieze.xmlutil:core-jvm:0.86.0")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:0.86.0")

    // Fuel (HTTP Client)
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "me.uippao.ifetch.MainKt"
    }
}
