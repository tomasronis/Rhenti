package com.tomasronis.rhentiapp.core.voip

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.tomasronis.rhentiapp.BuildConfig

/**
 * Manages audio routing for VoIP calls.
 */
class VoipAudioManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var previousAudioMode = AudioManager.MODE_NORMAL

    companion object {
        private const val TAG = "VoipAudioManager"
    }

    enum class AudioRoute {
        EARPIECE,
        SPEAKER,
        BLUETOOTH
    }

    init {
        requestAudioFocus()
    }

    /**
     * Set audio route
     */
    fun setAudioRoute(route: AudioRoute) {
        try {
            when (route) {
                AudioRoute.EARPIECE -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                }
                AudioRoute.SPEAKER -> {
                    audioManager.isSpeakerphoneOn = true
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                }
                AudioRoute.BLUETOOTH -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                }
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Audio route set to: $route")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to set audio route", e)
            }
        }
    }

    /**
     * Request audio focus
     */
    private fun requestAudioFocus() {
        previousAudioMode = audioManager.mode

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .build()

            audioFocusRequest?.let {
                audioManager.requestAudioFocus(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    /**
     * Abandon audio focus and restore previous state
     */
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }

        audioManager.mode = previousAudioMode
        audioManager.isSpeakerphoneOn = false

        if (audioManager.isBluetoothScoOn) {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
        }
    }

    /**
     * Cleanup audio resources
     */
    fun cleanup() {
        abandonAudioFocus()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Audio manager cleaned up")
        }
    }
}
