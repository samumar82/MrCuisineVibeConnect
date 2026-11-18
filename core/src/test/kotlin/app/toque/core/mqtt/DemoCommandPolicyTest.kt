package app.toque.core.mqtt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DemoCommandPolicyTest {
    private val topic = HaMqttSchema.demoCommandTopic()

    private fun action(mode: String = "demo", enabled: Boolean = true,
                       fresh: Boolean = true, retained: Boolean = false,
                       requestTopic: String = topic, payload: String = "STOP") =
        DemoCommandPolicy.authorize(enabled, mode, fresh, requestTopic, payload, retained)

    @Test fun onlyFreshOptInDemoCommandsAreAccepted() {
        assertEquals(DemoCommandPolicy.Action.STOP, action())
        assertEquals(DemoCommandPolicy.Action.PAUSE, action(payload = "PAUSE"))
    }

    @Test fun hardwareTwinAndUnexpectedInputsFailClosed() {
        assertNull(action(mode = "hardware"))
        assertNull(action(mode = "twin"))
        assertNull(action(mode = ""))
        assertNull(action(enabled = false))
        assertNull(action(fresh = false))
        assertNull(action(retained = true))
        assertNull(action(requestTopic = "toque_it/mcc/command"))
        assertNull(action(payload = "START"))
        assertNull(action(payload = "RESUME"))
        assertNull(action(payload = "HEAT"))
        assertNull(action(payload = "stop"))
        assertNull(action(payload = "STOP "))
    }
}
