package app.toque.core.mqtt

/**
 * Safety boundary for MQTT commands: only simulated-machine PAUSE and STOP.
 * Physical modes, retained broker messages, and unexpected topics/payloads
 * must never produce a command. This is not an emergency-stop mechanism.
 */
object DemoCommandPolicy {
    enum class Action { PAUSE, STOP }

    fun authorize(enabled: Boolean, modeKey: String, sampleFresh: Boolean,
                  topic: String, payload: String, retained: Boolean): Action? {
        if (!enabled || modeKey != "demo" || !sampleFresh || retained) return null
        if (topic != HaMqttSchema.demoCommandTopic()) return null
        return when (payload) {
            "PAUSE" -> Action.PAUSE
            "STOP" -> Action.STOP
            else -> null
        }
    }
}
