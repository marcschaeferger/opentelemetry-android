/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.android.instrumentation.pans

import android.os.Handler
import android.os.Looper
import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.Meter
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Initial skeleton for PANS instrumentation. Registers metric instruments and periodically
 * records stub values using a [PansStateCollector].
 */
@AutoService(AndroidInstrumentation::class)
class PansInstrumentation : AndroidInstrumentation {

    override val name: String = "pans"

    // Configurable (future public setters)
    internal var foregroundCollectionInterval: Duration = Duration.ofMinutes(30)

    private var meter: Meter? = null
    private var bytesSent: LongCounter? = null
    private var bytesReceived: LongCounter? = null
    private var preferenceSwitches: LongCounter? = null
    private var applyErrors: LongCounter? = null

    private val lastSnapshot = AtomicReference<PansSnapshot?>(null)
    private val installed = AtomicBoolean(false)

    private val handler = Handler(Looper.getMainLooper())
    private val scheduled = AtomicBoolean(false)

    private var collector: PansStateCollector = StubPansStateCollector()

    fun setForegroundCollectionInterval(interval: Duration): PansInstrumentation {
        if (!installed.get() && interval.toMillis() > 0) {
            foregroundCollectionInterval = interval
        }
        return this
    }

    /**
     * Internal/testing hook to override the state collector. Not part of the public API surface
     * (avoid exposing the collector interface until we finalize extensibility design).
     */
    internal fun setCollector(collector: PansStateCollector): PansInstrumentation {
        if (!installed.get()) {
            this.collector = collector
        }
        return this
    }

    override fun install(ctx: InstallationContext) {
        if (!installed.compareAndSet(false, true)) return

        val m = ctx.openTelemetry.meterProvider.get("io.opentelemetry.android.instrumentation.pans")
        meter = m
        bytesSent = m.counterBuilder("oem.network.bytes.sent").setUnit("By").build()
        bytesReceived = m.counterBuilder("oem.network.bytes.received").setUnit("By").build()
        preferenceSwitches = m.counterBuilder("oem.network.preference.switch.count").build()
        applyErrors = m.counterBuilder("oem.network.preference.apply.errors").build()

        // Observable gauge for current preference
        m.gaugeBuilder("oem.network.preference.current")
            .ofLongs()
            .setDescription("Active OEM network preference (1 for the active type)")
            .buildWithCallback { obs ->
                val snap = lastSnapshot.get()
                val pref = snap?.preferenceType ?: PreferenceType.UNKNOWN
                obs.record(1, Attributes.of(PREF_TYPE, pref.wire))
            }

        // Initial collection and schedule
        collectOnce()
        scheduleNext()
    }

    override fun uninstall(ctx: InstallationContext) {
        handler.removeCallbacksAndMessages(null)
        scheduled.set(false)
        installed.set(false)
        meter = null
        bytesSent = null
        bytesReceived = null
        preferenceSwitches = null
        applyErrors = null
        lastSnapshot.set(null)
    }

    private fun collectOnce() {
        try {
            val snapshot = collector.collect()
            val previous = lastSnapshot.getAndSet(snapshot)
            if (previous != null && previous.preferenceType != snapshot.preferenceType) {
                preferenceSwitches?.add(1, Attributes.of(PREF_FROM, previous.preferenceType.wire, PREF_TO, snapshot.preferenceType.wire))
            }
            // Delta bytes if cumulative present
            if (previous != null) {
                snapshot.txBytes?.let { tx -> previous.txBytes?.let { prev -> if (tx >= prev) bytesSent?.add(tx - prev, Attributes.empty()) else resetBaseline(previous, snapshot) } }
                snapshot.rxBytes?.let { rx -> previous.rxBytes?.let { prev -> if (rx >= prev) bytesReceived?.add(rx - prev, Attributes.empty()) else resetBaseline(previous, snapshot) } }
            }
        } catch (se: SecurityException) {
            applyErrors?.add(1, Attributes.of(ERROR_REASON, "permission_denied"))
        } catch (t: Throwable) {
            applyErrors?.add(1, Attributes.of(ERROR_REASON, "unknown"))
        }
    }

    private fun resetBaseline(@Suppress("UNUSED_PARAMETER") prev: PansSnapshot, @Suppress("UNUSED_PARAMETER") current: PansSnapshot) {
        // Treat as reset; no emission, baseline already updated in lastSnapshot. Parameters retained for potential future logic.
    }

    private fun scheduleNext() {
        if (scheduled.getAndSet(true)) return
        val delay = foregroundCollectionInterval.toMillis()
        handler.postDelayed(object : Runnable {
            override fun run() {
                scheduled.set(false)
                collectOnce()
                scheduleNext()
            }
        }, delay)
    }

    // Visible for tests (internal) to trigger an immediate collection cycle deterministically
    internal fun forceCollectForTest() {
        collectOnce()
    }

    companion object {
        private val PREF_TYPE = AttributeKey.stringKey("preference.type")
        private val PREF_FROM = AttributeKey.stringKey("from.preference.type")
        private val PREF_TO = AttributeKey.stringKey("to.preference.type")
        private val ERROR_REASON = AttributeKey.stringKey("reason")
    }
}
