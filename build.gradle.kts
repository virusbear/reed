plugins {
    id("org.jetbrains.dokka") version "1.5.0"
    kotlin("multiplatform") version "1.5.21"
}

group = "com.github.virusbear.reed"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

kotlin {
    jvm {
        compilations["main"].apply {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    mingwX64("native") {
        binaries {
            staticLib()
        }
    }

    sourceSets {
        val jvmMain by getting
        val nativeMain by getting
    }
}

dependencies {
    "commonMainImplementation"(kotlin("stdlib"))
    "jvmMainApi"("net.java.dev.jna:jna-platform:5.8.0")
}