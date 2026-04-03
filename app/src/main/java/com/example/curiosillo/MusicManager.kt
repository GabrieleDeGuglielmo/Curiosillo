package com.example.curiosillo

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log

class MusicManager(private val context: Context) {

    // Dual-player setup for gapless playback
    private var currentPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    // Playlist and tracking[cite: 2]
    private val playlist = listOf(R.raw.edu1, R.raw.edu2, R.raw.edu3)
    private var currentTrackIndex = 0

    // Define standard background volumes[cite: 2]
    private val standardVolume = 0.3f
    private val duckVolume = 0.1f

    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss of audio focus (e.g., another music app started playing)[cite: 2]
                stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Transient loss of audio focus (e.g., incoming phone call)[cite: 2]
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Another app needs to play a short sound (e.g., a notification)
                currentPlayer?.setVolume(duckVolume, duckVolume)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Audio focus regained[cite: 2]
                currentPlayer?.setVolume(standardVolume, standardVolume)
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
        if (currentPlayer == null) {
            setupInitialPlayer()
        }
        try {
            if (currentPlayer?.isPlaying == false) {
                currentPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Error during play", e)
        }
    }

    fun pause() {
        try {
            if (currentPlayer?.isPlaying == true) {
                currentPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Error during pause", e)
        }
    }

    fun stop() {
        try {
            // Release both players to free memory
            currentPlayer?.stop()
            currentPlayer?.release()
            currentPlayer = null

            nextPlayer?.stop()
            nextPlayer?.release()
            nextPlayer = null

            abandonAudioFocus()
        } catch (e: Exception) {
            Log.e("MusicManager", "Error during stop", e)
        }
    }

    private fun setupInitialPlayer() {
        currentPlayer?.release()

        // 1. Initialize the current player
        currentPlayer = MediaPlayer.create(context, playlist[currentTrackIndex]).apply {
            setVolume(standardVolume, standardVolume)
            setOnCompletionListener {
                shiftToNextTrack()
            }
        }

        // 2. Queue up the next player in the background
        prepareNextPlayer()
    }

    private fun prepareNextPlayer() {
        val nextIndex = (currentTrackIndex + 1) % playlist.size

        // Pre-load the next track
        nextPlayer = MediaPlayer.create(context, playlist[nextIndex]).apply {
            setVolume(standardVolume, standardVolume)
        }

        // Link the next player to the current one for seamless transition
        currentPlayer?.setNextMediaPlayer(nextPlayer)
    }

    private fun shiftToNextTrack() {
        // The current track has finished, and nextPlayer has automatically started.
        // Clean up the old player and shift variables.
        currentPlayer?.release()

        currentPlayer = nextPlayer
        currentTrackIndex = (currentTrackIndex + 1) % playlist.size

        // Set the listener on the new active player
        currentPlayer?.setOnCompletionListener {
            shiftToNextTrack()
        }

        // Prepare the following track to keep the loop going
        prepareNextPlayer()
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