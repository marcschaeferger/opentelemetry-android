/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.android.instrumentation.pans

import android.app.Application
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.time.Duration

class PansInstrumentationTest {

    private lateinit var application: Application
    private lateinit var sessionProvider: SessionProvider

    @Before
    fun setUp() {
        application = Mockito.mock(Application::class.java)
        sessionProvider = Mockito.mock(SessionProvider::class.java)
    }

    @Test
    fun emitsSwitchOnPreferenceChange() {
        val reader = InMemoryMetricReader.create()
        val meterProvider = SdkMeterProvider.builder().registerMetricReader(reader).build()
        val otel = OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build()

        val instrumentation = PansInstrumentation()
        val fakeCollector = object : PansStateCollector {
            private var step = 0
            override fun collect(): PansSnapshot {
                step++
                return when (step) {
                    1 -> PansSnapshot(PreferenceType.DEFAULT, 100, 200)
                    2 -> PansSnapshot(PreferenceType.OEM_PAID, 150, 260)
                    else -> PansSnapshot(PreferenceType.OEM_PAID, 180, 300)
                }
            }
        }
        instrumentation.setForegroundCollectionInterval(Duration.ofMillis(10))
        instrumentation.setCollector(fakeCollector)

    instrumentation.install(InstallationContext(application, otel, sessionProvider))

    // Deterministically trigger a few collection cycles instead of sleeping.
    repeat(3) { instrumentation.forceCollectForTest() }

        val metrics = reader.collectAllMetrics()
        assertThat(metrics).anySatisfy { metric ->
            if (metric.name == "oem.network.preference.switch.count") {
                assertThat(metric.longSumData.points).anySatisfy { pt ->
                    // Convert attribute map (AttributeKey -> value) into simple key->value map for ease of assertion
                    val flat = pt.attributes.asMap().entries.associate { (k, v) -> k.key to v }
                    assertThat(flat).containsEntry("from.preference.type", "default")
                    assertThat(flat).containsEntry("to.preference.type", "oem_paid")
                }
            }
        }
    }
}
