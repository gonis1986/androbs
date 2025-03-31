package com.gonis.androbs

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import org.webrtc.*

class WebRTCServer(
    private val port: Int,
    private val context: Context,
    private val rootEglBase: EglBase
) : NanoHTTPD(port) {

    private val pcFactory: PeerConnectionFactory
    private val peerConnection: PeerConnection

    init {
        // Inizializzazione di WebRTC
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        // Configurazione factory
        val encoderFactory = DefaultVideoEncoderFactory(
            rootEglBase.eglBaseContext,
            true,  // enableIntelVp8Encoder
            true    // enableH264HighProfile
        )

        val decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)

        pcFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        // Configurazione ICE servers
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = pcFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    // Gestisci il candidato ICE
                }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
        }) ?: throw IllegalStateException("Failed to create PeerConnection")
    }

    // ... resto del codice rimane uguale alla versione precedente
}