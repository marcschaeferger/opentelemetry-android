/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.android.instrumentation.pans

/**
 * Collector of PANS related state. Real implementation will query platform / dumpsys.
 */
internal interface PansStateCollector {
    fun collect(): PansSnapshot
}

internal data class PansSnapshot(
    val preferenceType: PreferenceType = PreferenceType.UNKNOWN,
    val txBytes: Long? = null,
    val rxBytes: Long? = null,
    val timestampMs: Long = System.currentTimeMillis(),
)

internal enum class PreferenceType(val wire: String) {
    OEM_PAID("oem_paid"),
    OEM_PRIVATE("oem_private"),
    DEFAULT("default"),
    UNKNOWN("unknown");
}

internal class StubPansStateCollector : PansStateCollector {
    override fun collect(): PansSnapshot = PansSnapshot()
}
