package com.gonis.androbs

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRTCStreamer(context: Context) {

    private var peerConnection: PeerConnection? = null
    private val eglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory

    init {
        val options = PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        // Creazione di peerConnection e implementazione dell'interfaccia PeerConnection.Observer
        peerConnection = peerConnectionFactory.createPeerConnection(iceServers, object : PeerConnection.Observer {
            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d("WebRTCStreamer", "onIceConnectionReceivingChange: $receiving")
            }

            // Implementa tutti gli altri metodi astratti richiesti
            override fun onIceCandidate(candidate: IceCandidate?) {
                Log.d("WebRTCStreamer", "onIceCandidate: ${candidate?.sdpMid}")
            }

            override fun onAddStream(stream: MediaStream?) {
                Log.d("WebRTCStreamer", "onAddStream: $stream")
            }

            override fun onRemoveStream(stream: MediaStream?) {
                Log.d("WebRTCStreamer", "onRemoveStream: $stream")
            }

            override fun onSignalingChange(signalingState: PeerConnection.SignalingState?) {
                Log.d("WebRTCStreamer", "onSignalingChange: $signalingState")
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Log.d("WebRTCStreamer", "onIceConnectionChange: $newState")
            }

            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
                Log.d("WebRTCStreamer", "onIceGatheringChange: $newState")
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d("WebRTCStreamer", "onConnectionChange: $newState")
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d("WebRTCStreamer", "onIceCandidatesRemoved: ${candidates?.size}")
            }

            override fun onDataChannel(dataChannel: DataChannel?) {
                Log.d("WebRTCStreamer", "onDataChannel: $dataChannel")
            }

            override fun onRenegotiationNeeded() {
                Log.d("WebRTCStreamer", "onRenegotiationNeeded")
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                Log.d("WebRTCStreamer", "onTrack: $transceiver")
            }
        })
    }
}
