package com.codewithkael.webrtcconference.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.util.Log
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


class WebRTCFactory @Inject constructor(
    private val context: Context,
    private val gson: Gson
) {

    private val eglBaseContext = EglBase.create().eglBaseContext
    private var permissionIntent: Intent? = null
    private lateinit var localSurfaceView: SurfaceViewRenderer

    private lateinit var rtcAudioManager: RTCAudioManager


    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }

    //    private val iceServer = listOf<PeerConnection.IceServer>()
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("turn:185.246.66.75:3478").setUsername("user")
            .setPassword("password").createIceServer(),
    )


    private var screenCapturer: VideoCapturer? = null
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }

    private val localTrackId = "local_track"
    private val localStreamId = "local_stream_android"
    private var videoCapturer: CameraVideoCapturer? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var localStream: MediaStream? = null
    private val TAG = "WebRTCFactory"

    fun init(surface: SurfaceViewRenderer) {
        rtcAudioManager = RTCAudioManager.create(context)
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        rtcAudioManager.start { selectedAudioDevice, availableAudioDevices ->
            Log.d(
                TAG,
                "init: selected $selectedAudioDevice  ,available ${availableAudioDevices.toList()}"
            )
        }
        CoroutineScope(Dispatchers.Default).launch {

            initPeerConnectionFactory(context)
        }
//        this.permissionIntent = permissionIntent
        initSurfaceView(surface)
    }

    private fun initSurfaceView(view: SurfaceViewRenderer) {
        this.localSurfaceView = view
        view.run {
            setMirror(false)
            setEnableHardwareScaler(true)
            init(eglBaseContext, null)
        }
//        startScreenCapturing(view)
        startLocalVideo(view)
    }

    private fun startLocalVideo(surface: SurfaceViewRenderer) {
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, eglBaseContext)
        videoCapturer = getVideoCapturer()
        videoCapturer?.initialize(
            surfaceTextureHelper,
            surface.context, localVideoSource.capturerObserver
        )
        videoCapturer?.startCapture(1080, 720, 30)
        localVideoTrack =
            peerConnectionFactory.createVideoTrack(localTrackId + "_video", localVideoSource)
        localVideoTrack?.addSink(surface)
        videoCapturer?.switchCamera(null)
        localAudioTrack =
            peerConnectionFactory.createAudioTrack(localTrackId + "_audio", localAudioSource)
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
        localStream?.addTrack(localAudioTrack)
        localStream?.addTrack(localVideoTrack)
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

    private fun startScreenCapturing(localSurfaceView: SurfaceViewRenderer) {
        CoroutineScope(Dispatchers.Default).launch {
            Log.d("TAG", "startScreenCapturing: called")
            val displayMetrics = DisplayMetrics()
            val windowsManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowsManager.defaultDisplay.getMetrics(displayMetrics)

            val screenWidthPixels = displayMetrics.widthPixels
            val screenHeightPixels = displayMetrics.heightPixels

            val surfaceTextureHelper = SurfaceTextureHelper.create(
                Thread.currentThread().name, eglBaseContext
            )

            screenCapturer = createScreenCapturer()
            screenCapturer!!.initialize(
                surfaceTextureHelper,
                context,
                localVideoSource.capturerObserver
            )
            screenCapturer!!.startCapture(screenWidthPixels, screenHeightPixels, 10)
//        screenCapturer!!.startCapture(720, 1080, 10)

            localVideoTrack =
                peerConnectionFactory.createVideoTrack(localTrackId + "_video", localVideoSource)
            localVideoTrack?.addSink(localSurfaceView)
            localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
            localStream?.addTrack(localVideoTrack)
            localStream?.addTrack(localAudioTrack) // Add the audio track to the local stream
        }
    }

    private fun createScreenCapturer(): VideoCapturer {
        return ScreenCapturerAndroid(permissionIntent, object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.d("TAG", "onStop: stopped screen casting permission")
            }
        })
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
        return connection?.let { RTCClientImpl(it, MyApplication.username, target, gson, listener) }
    }


}