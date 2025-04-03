package com.gonis.androbs

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.channels.ClosedByInterruptException
import java.net.NetworkInterface
import java.net.Inet4Address

class WebSocketServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {
    private var _isRunning: Boolean = false
    private val activeConnections = mutableSetOf<WebSocket>()
    private var localIpAddress: String = "0.0.0.0"

    val isRunning: Boolean
        get() = _isRunning

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        Log.d("WebSocketServer", "Nuova connessione da ${conn.remoteSocketAddress}")
        synchronized(activeConnections) {
            activeConnections.add(conn)
        }
        conn.send("""{"type":"connection","message":"Connessione stabilita","serverIp":"$localIpAddress"}""")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        Log.d("WebSocketServer", "Connessione chiusa: ${conn.remoteSocketAddress} - Codice: $code - Motivo: $reason")
        synchronized(activeConnections) {
            activeConnections.remove(conn)
        }
    }

    override fun onMessage(conn: WebSocket, message: String) {
        Log.d("WebSocketServer", "Messaggio ricevuto da ${conn.remoteSocketAddress}: $message")
        Log.d("WebSocket", "Messaggio ricevuto: $message")
        conn.send("""{"type":"echo","message":${message.toJsonString()},"serverIp":"$localIpAddress"}""")
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        when (ex) {
            is ClosedByInterruptException ->
                Log.d("WebSocketServer", "Server chiuso intenzionalmente")
            else ->
                Log.e("WebSocketServer", "Errore: ${ex?.message}", ex)
        }

        conn?.let {
            synchronized(activeConnections) {
                activeConnections.remove(it)
            }
            it.close(1001, "Errore interno")
        }
    }

    override fun onStart() {
        _isRunning = true
        localIpAddress = getLocalIpAddress()
        Log.d("WebSocketServer", "Server avviato su IP: $localIpAddress, Porta: ${address.port}")
        Log.d("WebSocketServer", "URL connessione: ws://$localIpAddress:${address.port}")
    }

    private fun getLocalIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address }
                ?.hostAddress ?: "0.0.0.0"
        } catch (ex: Exception) {
            Log.e("WebSocketServer", "Errore ottenimento IP", ex)
            "0.0.0.0"
        }
    }

    fun sendFrame(frameData: ByteArray) {
        // Invia i dati tramite WebSocket a tutti i client connessi
        for (client in connections) {
            client.send(frameData)
        }
    }

    override fun stop(timeout: Int) {
        try {
            synchronized(activeConnections) {
                activeConnections.forEach { conn ->
                    try {
                        conn.close(1000, "Server in arresto")
                    } catch (e: Exception) {
                        Log.e("WebSocketServer", "Errore chiusura connessione", e)
                    }
                }
                activeConnections.clear()
            }
            super.stop(timeout)
        } catch (e: Exception) {
            Log.e("WebSocketServer", "Errore durante l'arresto", e)
        } finally {
            _isRunning = false
            Log.d("WebSocketServer", "Server fermato")
        }
    }

    override fun broadcast(message: String) {
        synchronized(activeConnections) {
            activeConnections.forEach { conn ->
                try {
                    conn.send("""{"message":${message.toJsonString()},"serverIp":"$localIpAddress"}""")
                } catch (e: Exception) {
                    Log.e("WebSocketServer", "Errore broadcast", e)
                    activeConnections.remove(conn)
                }
            }
        }
    }

    fun getServerAddress(): String = "ws://$localIpAddress:${address.port}"
}

// Estensione per la sanitizzazione JSON
private fun String.toJsonString(): String {
    return this.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}