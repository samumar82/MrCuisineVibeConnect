package app.toque.core.mqtt

import app.toque.core.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HaMqttSchemaTest {
    @Test fun telemetryIsReadOnlyAndJsonValid() {
        val payload = HaMqttSchema.state(HaMqttSchema.Measurement(
            actualTemperatureC = 75, targetTemperatureC = 80,
            actualRpm = 300, weightG = -12, motorRunning = true,
            heaterRunning = false, lidOpen = false, sampledAtMs = 12345L
        ))
        val data = Json.parse(payload) as Map<*, *>
        assertEquals(75L, data["actual_temperature_c"])
        assertEquals(-12L, data["weight_g"])
        assertEquals(true, data["motor_running"])
        assertEquals(12345L, data["sampled_at_ms"])
        assertFalse(data.containsKey("command"))
    }

    @Test fun discoveryUsesOnlyStateAndAvailabilityTopics() {
        assertEquals(9, HaMqttSchema.sensors.size)
        for (sensor in HaMqttSchema.sensors) {
            val config = Json.parse(HaMqttSchema.config(sensor)) as Map<*, *>
            assertEquals(HaMqttSchema.stateTopic, config["state_topic"])
            assertEquals(HaMqttSchema.availabilityTopic, config["availability_topic"])
            assertFalse(config.containsKey("command_topic"))
            assertTrue(HaMqttSchema.configTopic(sensor).endsWith("/config"))
        }
    }

    @Test fun staleSamplesAreNeverFresh() {
        val sample = HaMqttSchema.Measurement(sampledAtMs = 1000L)
        assertTrue(HaMqttSchema.valid(sample, 3500L))
        assertFalse(HaMqttSchema.valid(sample, 4500L))
        assertFalse(HaMqttSchema.valid(sample, 500L))
        assertFalse(HaMqttSchema.valid(sample.copy(sampledAtMs = 0), 100L))
    }
    @Test fun demoButtonsAreExplicitlyLimitedToPauseAndStop() {
        assertEquals(listOf("PAUSE", "STOP"), HaMqttSchema.demoButtons.map { it.command })
        assertEquals("toque_it/mcc/demo/command", HaMqttSchema.demoCommandTopic())
        for (button in HaMqttSchema.demoButtons) {
            val config = Json.parse(HaMqttSchema.demoButtonConfig(button)) as Map<*, *>
            assertEquals(HaMqttSchema.demoCommandTopic(), config["command_topic"])
            assertEquals(button.command, config["payload_press"])
            assertTrue(HaMqttSchema.demoButtonConfigTopic(button).startsWith("homeassistant/button/"))
        }
    }

}
