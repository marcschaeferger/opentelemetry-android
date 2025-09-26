# PANS Instrumentation

Status: Experimental (initial skeleton)

This module will collect metrics related to OEM Per-App Network Selection (PANS) on supported
Android devices/builds. Initial version provides the scaffolding only.

## Planned Metrics

- Active OEM network preference (gauge)
- Preference switches (counter)
- Bytes sent / received (counters) attributed to preference where possible
- Apply errors (counter)

## Current State

No real data collection yet; metrics instruments are registered with stub values.

## Installation

Add the dependency (or use the `instrumentation-all` bundle once integrated):

```kotlin
implementation("io.opentelemetry.android:instrumentation-pans:<version>")
```

## Configuration (future)

- Foreground collection interval
- Optional background collection

## Limitations

Requires privileged APIs for real PANS data; gracefully degrades to no-op if unavailable.
