package app.toque.core.mqtt

import app.toque.core.machine.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HaSnapshotAdapterTest {
    @Test fun validSnapshotProvidesMeasurementsWithoutConflatingSpeedAndRpm() {
        val snapshot = Snapshot(
            transportUp = true,
            hasSample = true,
            sampleAgeMs = 150L,
            actualTempC = 72,
            targetTempC = 80,
            rpm = 901,
            jobSpeed = 3,
            weightG = -5,
            motorOn = true,
            heaterOn = true,
            lidOpen = false,
            boardErrorCode = 0
        )
        val measurement = HaSnapshotAdapter.fromSnapshot(snapshot, 12_000L)
        assertNotNull(measurement)
        assertEquals(72, measurement!!.actualTemperatureC)
        assertEquals(901, measurement.actualRpm)
        assertEquals(3, measurement.speedLevel)
        assertEquals(-5, measurement.weightG)
        assertEquals(11_850L, measurement.sampledAtMs)
        assertTrue(HaMqttSchema.valid(measurement, 12_000L))
    }

    @Test fun missingOrOldBoardSamplesAreRejected() {
        assertNull(HaSnapshotAdapter.fromSnapshot(Snapshot(), 10_000L))
        assertNull(HaSnapshotAdapter.fromSnapshot(
            Snapshot(transportUp = false, hasSample = true, sampleAgeMs = 50L), 10_000L))
        assertNull(HaSnapshotAdapter.fromSnapshot(
            Snapshot(transportUp = true, hasSample = true, sampleAgeMs = 3_001L), 10_000L))
        assertNull(HaSnapshotAdapter.fromSnapshot(
            Snapshot(transportUp = true, hasSample = true, sampleAgeMs = -1L), 10_000L))
    }
}
