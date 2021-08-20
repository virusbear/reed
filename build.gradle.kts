plugins {
    kotlin("jvm") version "1.5.21"
}

group = "com.github.virusbear.reed"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
    implementation("net.java.dev.jna:jna-platform:5.8.0")
}