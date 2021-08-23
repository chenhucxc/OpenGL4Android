package com.dev.nativegl.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper

/**
 * description : clone from https://github.com/githubhaohao/NDK_OpenGLES_3_0
 *
 * @author     : hudongxin
 * @date       : 8/11/21
 */
class AudioCollector : AudioRecord.OnRecordPositionUpdateListener {
    companion object {
        private const val TAG = "AudioRecorderWrapper"
        private const val RECORDER_SAMPLE_RATE = 44100
        private const val RECORDER_CHANNELS = 1
        private const val RECORDER_ENCODING_BIT = 16
        private const val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    private var mAudioRecord: AudioRecord? = null
    private var mThread: Thread? = null
    private lateinit var mAudioBuffer: ShortArray
    private var mHandler: Handler? = null
    private var mBufferSize = 0
    private var mCallback: Callback? = null

    fun AudioCollector() {
        mBufferSize = 2 * AudioRecord.getMinBufferSize(
            RECORDER_SAMPLE_RATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING
        )
    }

    @SuppressLint("MissingPermission")
    fun init() {
        mAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, RECORDER_SAMPLE_RATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, mBufferSize
        )
        mAudioRecord!!.startRecording()
        mThread = object : Thread("Audio-Recorder") {
            override fun run() {
                super.run()
                mAudioBuffer = ShortArray(mBufferSize)
                Looper.prepare()
                mHandler = Handler(Looper.myLooper()!!)
                mAudioRecord!!.setRecordPositionUpdateListener(this@AudioCollector, mHandler)
                val bytePerSample = RECORDER_ENCODING_BIT / 8
                val samplesToDraw = (mBufferSize / bytePerSample).toFloat()
                mAudioRecord!!.positionNotificationPeriod = samplesToDraw.toInt()
                mAudioRecord!!.read(mAudioBuffer, 0, mBufferSize)
                Looper.loop()
            }
        }
        mThread?.start()
    }

    fun unInit() {
        if (mAudioRecord != null) {
            mAudioRecord!!.stop()
            mAudioRecord!!.release()
            mHandler!!.looper.quitSafely()
            mAudioRecord = null
        }
    }

    fun addCallback(callback: Callback?) {
        mCallback = callback
    }

    override fun onMarkerReached(recorder: AudioRecord?) {

    }

    override fun onPeriodicNotification(recorder: AudioRecord?) {
        if (mAudioRecord != null && mAudioRecord!!.recordingState == AudioRecord.RECORDSTATE_RECORDING && mAudioRecord!!.read(
                mAudioBuffer,
                0,
                mAudioBuffer.size
            ) != -1
        ) {
            if (mCallback != null) mCallback!!.onAudioBufferCallback(mAudioBuffer)
        }
    }

    interface Callback {
        fun onAudioBufferCallback(buffer: ShortArray?) // little-endian
    }
}