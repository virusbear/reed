import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    implementation(kotlin("stdlib"))
    api("net.java.dev.jna:jna-platform:5.8.0")
}