plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android PANS (Per-App Network Selection) instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.pans"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(project(":instrumentation:android-instrumentation"))
    implementation(project(":services"))
    implementation(project(":common"))
    implementation(project(":session"))
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
    implementation(libs.androidx.core)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
    implementation(libs.opentelemetry.semconv.incubating)
    implementation(libs.opentelemetry.sdk.extension.incubator)
    testImplementation(project(":test-common"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
