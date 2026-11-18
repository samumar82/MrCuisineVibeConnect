package app.toque.mqtt

import android.app.Service
import android.content.Intent
import android.os.IBinder
import app.toque.Prefs
import app.toque.core.machine.Snapshot
import app.toque.core.mqtt.HaMqttSchema
import app.toque.core.mqtt.HaSnapshotAdapter
import app.toque.machine.MachineClient
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Explicitly opt-in read-only MQTT export. Network operations run on one worker,
 * never on the UI thread and never inside the independent motor-control process.
 * There is no MQTT subscription and no command ingress.
 */
class HaTelemetryService : Service() {
    private lateinit var prefs: Prefs
    private lateinit var machine: MachineClient
    private val executor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "toque-ha-mqtt").apply { isDaemon = true }
    }
    @Volatile private var latest: Snapshot = Snapshot()
    private var unsubscribe: (() -> Unit)? = null
    private var mqtt: MqttPublisher? = null
    private var lastPing = 0L
    private var lastDiscovery = 0L
    private var lastState = 0L
    private var lastOnline = false

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        machine = MachineClient(this)
        unsubscribe = machine.onSnapshot { latest = it }
        machine.connect()
        executor.scheduleWithFixedDelay({ tick() }, 0, 2, TimeUnit.SECONDS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefs.mqttEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun tick() {
        if (!prefs.mqttEnabled || prefs.mqttHost.isBlank()) {
            closeMqtt()
            return
        }
        try {
            val now = System.currentTimeMillis()
            if (mqtt == null) {
                val next = MqttPublisher()
                try {
                    next.connect(prefs.mqttHost, prefs.mqttPort, prefs.mqttTls,
                        "toque-it-mcc", prefs.mqttUser, prefs.mqttPassword)
                    mqtt = next
                    lastPing = now
                    lastDiscovery = 0L
                    lastState = 0L
                    lastOnline = false
                } catch (e: Exception) {
                    next.close()
                    throw e
                }
            }
            val client = mqtt ?: return
            if (lastDiscovery == 0L || now - lastDiscovery >= 15 * 60_000L) {
                for (sensor in HaMqttSchema.sensors) {
                    client.publish(HaMqttSchema.configTopic(sensor), HaMqttSchema.config(sensor), retain = true)
                }
                lastDiscovery = now
            }
            val sample = HaSnapshotAdapter.fromSnapshot(latest, now)
            val valid = sample != null && HaMqttSchema.valid(sample, now)
            if (valid && sample != null) {
                if (!lastOnline) {
                    client.publish(HaMqttSchema.availabilityTopic, "online", retain = true)
                    lastOnline = true
                }
                if (lastState == 0L || now - lastState >= 2_000L) {
                    client.publish(HaMqttSchema.stateTopic, HaMqttSchema.state(sample))
                    lastState = now
                }
            } else if (lastOnline || lastState == 0L) {
                client.publish(HaMqttSchema.availabilityTopic, "offline", retain = true)
                lastOnline = false
                lastState = now
            }
            if (now - lastPing >= 20_000L) {
                client.ping()
                lastPing = now
            }
        } catch (_: Exception) {
            // Intentional: a network failure must never propagate to the controller.
            closeMqtt()
        }
    }

    private fun closeMqtt() {
        val client = mqtt
        mqtt = null
        if (client != null) {
            try { client.publish(HaMqttSchema.availabilityTopic, "offline", retain = true) }
            catch (_: Exception) {}
            client.close()
        }
        lastOnline = false
        lastDiscovery = 0
    }

    override fun onDestroy() {
        executor.shutdownNow()
        unsubscribe?.invoke()
        closeMqtt()
        super.onDestroy()
    }
}
