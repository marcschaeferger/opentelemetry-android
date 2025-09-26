import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.plugin)
        classpath(libs.byteBuddy.plugin)
    }
}

plugins {
    alias(libs.plugins.publishPlugin)
    id("org.jetbrains.kotlinx.kover")
}

// Updated baseline to Java 21 (LTS) / Kotlin 2.2 per request to remove 1.9 deprecation warnings
extra["java_version"] = JavaVersion.VERSION_21
extra["jvm_target"] = JvmTarget.JVM_21
extra["kotlin_min_supported_version"] = KotlinVersion.KOTLIN_2_2

allprojects {
    repositories {
        google()
        mavenCentral()
    }
    if (findProperty("final") != "true") {
        version = "$version-SNAPSHOT"
    }
}

nexusPublishing {
    repositories {
        // see https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuration
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("SONATYPE_USER"))
            password.set(System.getenv("SONATYPE_KEY"))
        }
    }
}

kover {
    merge {
        subprojects { project ->
            true
        }
    }
    reports {
        filters {
            excludes {
                androidGeneratedClasses()
                classes("*.BuildConfig")
            }
        }
    }
}
