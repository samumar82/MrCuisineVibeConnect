package app.toque.mqtt

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
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
    private val main = Handler(Looper.getMainLooper())
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
    private var lastStatus = ""

    private fun report(message: String) {
        if (message == lastStatus) return
        lastStatus = message
        prefs.mqttStatus = message
        prefs.mqttStatusAt = System.currentTimeMillis()
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        machine = MachineClient(this)
        unsubscribe = machine.onSnapshot { latest = it }
        machine.connect()
        report("MQTT in avvio: attendo il broker")
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
            report(if (prefs.mqttEnabled) "MQTT: indirizzo broker mancante" else "MQTT disattivato")
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
                    report("Broker MQTT connesso: invio Discovery")
                    lastPing = now
                    lastDiscovery = 0L
                    lastState = 0L
                    lastOnline = false
                    if (demoControlsAllowed()) next.subscribe(HaMqttSchema.demoCommandTopic())
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
                // Empty retained configs remove Demo buttons when remote control is disabled.
                for (button in HaMqttSchema.demoButtons) {
                    client.publish(
                        HaMqttSchema.demoButtonConfigTopic(button),
                        if (demoControlsAllowed()) HaMqttSchema.demoButtonConfig(button) else "",
                        retain = true,
                    )
                }
                lastDiscovery = now
                report("Broker connesso: Discovery inviata, attendo telemetria valida")
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
                    report("MQTT OK: stato macchina pubblicato")
                }
            } else if (lastOnline || lastState == 0L) {
                client.publish(HaMqttSchema.availabilityTopic, "offline", retain = true)
                lastOnline = false
                lastState = now
                report("Broker connesso: macchina senza campioni validi (offline)")
            }
            client.poll()?.let { command -> handleDemoCommand(command) }
            if (now - lastPing >= 20_000L) {
                client.ping()
                lastPing = now
            }
        } catch (e: Exception) {
            // No secrets in diagnostics; avoid echoing broker exception messages.
            val reason = when (e) {
                is java.net.UnknownHostException -> "indirizzo broker non risolto (DNS)"
                is java.net.ConnectException -> "connessione TCP rifiutata (host/porta)"
                is java.net.SocketTimeoutException -> "timeout di rete o risposta MQTT mancante"
                is javax.net.ssl.SSLException -> "errore TLS/certificato o porta errata"
                is IllegalStateException -> if ((e.message ?: "").startsWith("MQTT connection rejected:")) {
                    "autenticazione o accesso MQTT rifiutato (CONNACK)"
                } else "handshake MQTT fallito"
                else -> "errore MQTT (" + e.javaClass.simpleName + ")"
            }
            report("MQTT non connesso: " + reason)
            closeMqtt()
        }
    }

    private fun demoControlsAllowed(): Boolean =
        prefs.mqttDemoControlEnabled && prefs.mode == Prefs.Mode.DEMO && prefs.mqttEnabled

    /** Never execute remote commands in HARDWARE/TWIN mode, even after a mode change. */
    private fun handleDemoCommand(message: MqttPublisher.Received) {
        if (!demoControlsAllowed() || message.retained ||
            message.topic != HaMqttSchema.demoCommandTopic()) return
        if (message.payload != "PAUSE" && message.payload != "STOP") return
        main.post {
            if (!demoControlsAllowed()) return@post
            val s = machine.snapshot
            if (!s.hasSample || !s.transportUp) return@post
            if (message.payload == "PAUSE" && s.running) machine.pause {}
            else if (message.payload == "STOP" && s.busy) machine.stop {}
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
        report("MQTT arrestato")
        super.onDestroy()
    }
}
