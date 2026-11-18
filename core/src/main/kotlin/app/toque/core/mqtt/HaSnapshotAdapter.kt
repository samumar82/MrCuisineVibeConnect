package app.toque.core.mqtt

import app.toque.core.machine.Snapshot

/**
 * Converts an existing Toque snapshot into a read-only telemetry model.
 *
 * IMPORTANT: snapshot.sampleAgeMs is the age of the latest physical/simulated
 * board response, not the age of the Binder callback. Do not refresh sample
 * timestamps just because the UI received another callback.
 *
 * This adapter never opens the serial port and never sends a motor command.
 */
object HaSnapshotAdapter {
    fun fromSnapshot(s: Snapshot, nowMs: Long): HaMqttSchema.Measurement? {
        if (!s.hasSample || !s.transportUp || s.sampleAgeMs < 0L || s.sampleAgeMs > 3_000L) {
            return null
        }
        if (nowMs <= s.sampleAgeMs) return null
        return HaMqttSchema.Measurement(
            actualTemperatureC = s.actualTempC,
            // Snapshot exposes the target in degrees C.
            targetTemperatureC = s.targetTempC,
            actualRpm = s.rpm,
            // Toque exposes the selected speed as a level (0..10), not target RPM.
            speedLevel = s.jobSpeed,
            weightG = s.weightG,
            motorRunning = s.motorOn,
            heaterRunning = s.heaterOn,
            lidOpen = s.lidOpen,
            boardErrorCode = s.boardErrorCode,
            sampledAtMs = nowMs - s.sampleAgeMs,
        )
    }
}
