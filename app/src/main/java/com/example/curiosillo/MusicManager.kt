package com.example.curiosillo

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log

class MusicManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val playlist = listOf(R.raw.edu1, R.raw.edu2, R.raw.edu3)
    private var currentTrackIndex = 0

    // Define a standard background volume
    private val standardVolume = 0.3f
    private val duckVolume = 0.1f

    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss of audio focus (e.g., another music app started playing)
                stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Transient loss of audio focus (e.g., incoming phone call)
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Another app needs to play a short sound (e.g., a notification)
                mediaPlayer?.setVolume(duckVolume, duckVolume)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Audio focus regained
                mediaPlayer?.setVolume(standardVolume, standardVolume)
                play()
            }
        }
    }

    fun start() {
        if (requestAudioFocus()) {
            play()
        }
    }

    private fun play() {
        if (mediaPlayer == null) {
            setupMediaPlayer()
        }
        try {
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Error during play", e)
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Error during pause", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            abandonAudioFocus()
        } catch (e: Exception) {
            Log.e("MusicManager", "Error during stop", e)
        }
    }

    private fun setupMediaPlayer() {
        // CRITICAL FIX: Release the existing player before instantiating a new one
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer.create(context, playlist[currentTrackIndex])
        mediaPlayer?.isLooping = false
        mediaPlayer?.setVolume(standardVolume, standardVolume)

        mediaPlayer?.setOnCompletionListener {
            currentTrackIndex = (currentTrackIndex + 1) % playlist.size
            setupMediaPlayer()
            mediaPlayer?.start()
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(afChangeListener)
                .build()

            audioManager.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(afChangeListener)
        }
    }
}