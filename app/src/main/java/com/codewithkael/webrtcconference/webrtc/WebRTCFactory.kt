package com.codewithkael.webrtcconference.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.DisplayMetrics

import android.view.WindowManager
import com.codewithkael.webrtcconference.utils.MyApplication
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WebRTCFactory @Inject constructor(
    private val context: Context,
    private val gson: Gson
) {

    private lateinit var localStreamListener: LocalStreamListener
    private val eglBaseContext = EglBase.create().eglBaseContext
    private lateinit var rtcAudioManager: RTCAudioManager

    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("turn:185.246.66.75:3478").setUsername("user")
            .setPassword("password").createIceServer(),
    )

    private var screenCapturer: VideoCapturer? = null
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }

    private val localTrackId = "local_track"
    private val localStreamId = "${MyApplication.username}_local_stream_android"
    private var videoCapturer: CameraVideoCapturer? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var localStream: MediaStream? = null


    fun init(surface: SurfaceViewRenderer, localStreamListener: LocalStreamListener) {
        this.localStreamListener = localStreamListener
        rtcAudioManager = RTCAudioManager.create(context)
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        rtcAudioManager.start { _, _ -> }
        initPeerConnectionFactory(context)
        initSurfaceView(surface)
    }

    private fun initSurfaceView(view: SurfaceViewRenderer) {
        view.run {
            setMirror(false)
            setEnableHardwareScaler(true)
            init(eglBaseContext, null)
        }
        startLocalVideo(view)
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        view.run {
            setMirror(false)
            setEnableHardwareScaler(true)
            init(eglBaseContext, null)
        }
    }

    private fun startLocalVideo(surface: SurfaceViewRenderer) {
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, eglBaseContext)
        videoCapturer = getVideoCapturer()
        videoCapturer?.initialize(
            surfaceTextureHelper,
            surface.context, localVideoSource.capturerObserver
        )
        videoCapturer?.startCapture(480, 320, 10)
        localVideoTrack =
            peerConnectionFactory.createVideoTrack(localTrackId + "_video", localVideoSource)
        localVideoTrack?.addSink(surface)
        videoCapturer?.switchCamera(null)
        localAudioTrack =
            peerConnectionFactory.createAudioTrack(localTrackId + "_audio", localAudioSource)
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
        localStream?.addTrack(localAudioTrack)
        localStream?.addTrack(localVideoTrack)
        localStreamListener.onLocalStreamReady(localStream!!)

    }

    private fun getVideoCapturer(): CameraVideoCapturer {
        return Camera2Enumerator(context).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }
    }


    private fun initPeerConnectionFactory(application: Context) {
        val options = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder().setVideoDecoderFactory(
            DefaultVideoDecoderFactory(eglBaseContext)
        ).setVideoEncoderFactory(
            DefaultVideoEncoderFactory(
                eglBaseContext, true, true
            )
        ).setOptions(PeerConnectionFactory.Options().apply {
            disableEncryption = false
            disableNetworkMonitor = false
        }).createPeerConnectionFactory()
    }

    fun onDestroy() {
        runCatching {
            screenCapturer?.stopCapture()
            screenCapturer?.dispose()
            localStream?.dispose()
        }
    }


    fun createRtcClient(
        observer: PeerConnection.Observer, target: String,
        listener: WebRTCSignalListener
    ): RTCClient? {
        val connection = peerConnectionFactory.createPeerConnection(
            PeerConnection.RTCConfiguration(iceServer).apply {
                enableCpuOveruseDetection = true
            }, observer
        )
        connection?.addStream(localStream)
        return connection?.let {
            RTCClientImpl(it, MyApplication.username, target, gson, listener) {
                destroyProcess()
            }
        }
    }

    private fun destroyProcess() {
        runCatching {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        }
    }


}