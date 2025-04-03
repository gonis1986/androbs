package com.gonis.androbs  // Assicurati che sia la prima riga del file

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Inet4Address
import java.nio.channels.ClosedByInterruptException

class WebRTCWebSocketServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {
    private val activeConnections = mutableSetOf<WebSocket>()
    private var localIpAddress: String = "0.0.0.0"

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        Log.d("WebRTCWebSocketServer", "Nuova connessione da ${conn.remoteSocketAddress}")
        synchronized(activeConnections) {
            activeConnections.add(conn)
        }
        conn.send("""{"type":"connection","message":"Connessione stabilita","serverIp":"$localIpAddress"}""")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        Log.d("WebRTCWebSocketServer", "Connessione chiusa: ${conn.remoteSocketAddress} - Codice: $code - Motivo: $reason")
        synchronized(activeConnections) {
            activeConnections.remove(conn)
        }
    }

    override fun onMessage(conn: WebSocket, message: String) {
        Log.d("WebRTCWebSocketServer", "Messaggio ricevuto: $message")
        conn.send("""{"type":"echo","message":${message.toJsonString()},"serverIp":"$localIpAddress"}""")
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        Log.e("WebRTCWebSocketServer", "Errore: ${ex?.message}", ex)
        conn?.close(1001, "Errore interno")
    }

    override fun onStart() {
        localIpAddress = getLocalIpAddress()
        Log.d("WebRTCWebSocketServer", "Server WebSocket avviato su ws://$localIpAddress:${address.port}")
    }

    private fun getLocalIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address }
                ?.hostAddress ?: "0.0.0.0"
        } catch (ex: Exception) {
            Log.e("WebRTCWebSocketServer", "Errore ottenimento IP", ex)
            "0.0.0.0"
        }
    }

    fun getServerAddress(): String = "ws://$localIpAddress:${address.port}"
}

// Estensione per gestire stringhe JSON correttamente
private fun String.toJsonString(): String {
    return this.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
