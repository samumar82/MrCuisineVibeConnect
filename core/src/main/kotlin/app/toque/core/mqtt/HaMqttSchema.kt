package app.toque.core.mqtt

import app.toque.core.json.Json

/**
 * Read-only Home Assistant MQTT payload schema.
 *
 * This module deliberately has no MQTT client, socket, serial access or command handler.
 * Network publication must happen asynchronously outside the motor-control loop.
 */
object HaMqttSchema {
    const val prefix = "toque_it/mcc"
    const val stateTopic = "$prefix/state"
    const val availabilityTopic = "$prefix/availability"

    data class Measurement(
        val actualTemperatureC: Int? = null,
        val targetTemperatureC: Int? = null,
        val actualRpm: Int? = null,
        val speedLevel: Int? = null,
        val weightG: Int? = null,
        val motorRunning: Boolean? = null,
        val heaterRunning: Boolean? = null,
        val lidOpen: Boolean? = null,
        val boardErrorCode: Int? = null,
        val sampledAtMs: Long,
    )

    /**
     * Reject stale telemetry instead of inventing values from cached/invalid frames.
     * Caller must use the timestamp when a valid frame was RECEIVED, not callback time.
     */
    fun valid(sample: Measurement, nowMs: Long, maxAgeMs: Long = 3_000L): Boolean =
        maxAgeMs > 0L && sample.sampledAtMs > 0L &&
            nowMs >= sample.sampledAtMs && nowMs - sample.sampledAtMs <= maxAgeMs

    fun state(sample: Measurement): String = Json.write(linkedMapOf(
        "actual_temperature_c" to sample.actualTemperatureC,
        "target_temperature_c" to sample.targetTemperatureC,
        "actual_rpm" to sample.actualRpm,
        "speed_level" to sample.speedLevel,
        "weight_g" to sample.weightG,
        "motor_running" to sample.motorRunning,
        "heater_running" to sample.heaterRunning,
        "lid_open" to sample.lidOpen,
        "board_error_code" to sample.boardErrorCode,
        "sampled_at_ms" to sample.sampledAtMs,
    ))

    data class Sensor(val key: String, val name: String, val unit: String? = null,
                      val deviceClass: String? = null, val stateClass: String? = null,
                      val binary: Boolean = false)

    val sensors = listOf(
        Sensor("actual_temperature_c", "Temperatura reale", "°C", "temperature", "measurement"),
        Sensor("target_temperature_c", "Temperatura impostata", "°C", "temperature"),
        Sensor("actual_rpm", "Velocità reale", "rpm", stateClass = "measurement"),
        Sensor("speed_level", "Livello velocità", stateClass = "measurement"),
        Sensor("weight_g", "Peso", "g", "weight", "measurement"),
        Sensor("motor_running", "Motore attivo", binary = true),
        Sensor("heater_running", "Riscaldamento attivo", binary = true),
        Sensor("lid_open", "Coperchio aperto", binary = true),
        Sensor("board_error_code", "Codice errore"),
    )

    fun configTopic(sensor: Sensor): String =
        "homeassistant/${if (sensor.binary) "binary_sensor" else "sensor"}/toque_it_mcc_${sensor.key}/config"

    /** JSON configuration payload compatible with MQTT Discovery. No command_topic. */
    fun config(sensor: Sensor): String {
        val data = linkedMapOf<String, Any?>(
            "name" to sensor.name,
            "unique_id" to "toque_it_mcc_${sensor.key}",
            "state_topic" to stateTopic,
            "availability_topic" to availabilityTopic,
            "payload_available" to "online",
            "payload_not_available" to "offline",
            "value_template" to "{{ value_json.${sensor.key} }}",
            "device" to linkedMapOf(
                "identifiers" to listOf("toque_it_mcc"),
                "name" to "Monsieur Cuisine Connect",
                "manufacturer" to "SilverCrest",
                "model" to "SKMC 1200 / Toque IT",
            ),
        )
        if (sensor.unit != null) data["unit_of_measurement"] = sensor.unit
        if (sensor.deviceClass != null) data["device_class"] = sensor.deviceClass
        if (sensor.stateClass != null) data["state_class"] = sensor.stateClass
        if (sensor.binary) {
            data["payload_on"] = "true"
            data["payload_off"] = "false"
            // State payload uses JSON booleans, rendered as strings by Jinja template.
            data["value_template"] = "{{ 'true' if value_json.${sensor.key} else 'false' }}"
        }
        return Json.write(data)
    }
}
