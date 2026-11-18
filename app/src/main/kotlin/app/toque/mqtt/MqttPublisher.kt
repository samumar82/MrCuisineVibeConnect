package app.toque.mqtt

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/** MQTT 3.1.1 QoS0 publisher: deliberately no subscriptions or command receiver.
 * All operations are blocking and must run on a dedicated network thread.
 */
internal class MqttPublisher : AutoCloseable {
    private var socket: Socket? = null
    private var input: BufferedInputStream? = null
    private var output: BufferedOutputStream? = null

    @Synchronized
    fun connect(host: String, port: Int, tls: Boolean, clientId: String,
                username: String, password: String) {
        close()
        val raw = Socket()
        try {
            raw.connect(InetSocketAddress(host, port), 3_000)
            raw.soTimeout = 5_000
            val conn = if (tls) {
                val ssl = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                    .createSocket(raw, host, port, true) as SSLSocket
                ssl.startHandshake()
                ssl
            } else raw
            conn.soTimeout = 5_000
            socket = conn
            input = BufferedInputStream(conn.getInputStream())
            output = BufferedOutputStream(conn.getOutputStream())
            require(username.isNotEmpty() || password.isEmpty()) { "MQTT username required with password" }
            val flags = 0x02 or 0x04 or 0x20 or
                (if (username.isNotEmpty()) 0x80 else 0) or
                (if (password.isNotEmpty()) 0x40 else 0)
            val body = ByteArrayOutputStream()
            utf8(body, "MQTT")
            body.write(4)
            body.write(flags)
            body.write(0); body.write(45)
            utf8(body, clientId)
            utf8(body, "toque_it/mcc/availability")
            utf8(body, "offline")
            if (username.isNotEmpty()) utf8(body, username)
            if (password.isNotEmpty()) utf8(body, password)
            packet(0x10, body.toByteArray())
            val response = ByteArray(4)
            var off = 0
            while (off < response.size) {
                val n = input!!.read(response, off, response.size - off)
                if (n < 0) throw IllegalStateException("MQTT CONNACK missing")
                off += n
            }
            if (response[0].toInt() != 0x20 || response[1].toInt() != 0x02 ||
                response[2].toInt() != 0 || response[3].toInt() != 0)
                throw IllegalStateException("MQTT connection rejected: " + (response[3].toInt() and 255))
        } catch (t: Throwable) {
            close()
            throw t
        }
    }

    @Synchronized
    fun publish(topic: String, payload: String, retain: Boolean = false) {
        val b = ByteArrayOutputStream()
        utf8(b, topic)
        b.write(payload.toByteArray(Charsets.UTF_8))
        packet(if (retain) 0x31 else 0x30, b.toByteArray())
    }

    @Synchronized
    fun ping() = packet(0xC0, ByteArray(0))

    private fun packet(header: Int, payload: ByteArray) {
        val out = output ?: throw IllegalStateException("MQTT disconnected")
        out.write(header)
        var n = payload.size
        do {
            var digit = n % 128
            n /= 128
            if (n > 0) digit = digit or 0x80
            out.write(digit)
        } while (n > 0)
        out.write(payload)
        out.flush()
    }

    private fun utf8(out: ByteArrayOutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        require(bytes.size <= 65535)
        out.write(bytes.size ushr 8)
        out.write(bytes.size and 255)
        out.write(bytes)
    }

    @Synchronized
    override fun close() {
        try { output?.close() } catch (_: Exception) {}
        try { input?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        output = null
        input = null
        socket = null
    }
}
