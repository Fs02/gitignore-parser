import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "1.9.21"
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_6)
        apiVersion.set(KotlinVersion.KOTLIN_1_6)
    }
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest("1.9.21")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
//            groupId = "com.github.fs02"
//            artifactId = "gitignore-parser"
//            version = "0.1.0"

            from(components["java"])
        }
    }
}
