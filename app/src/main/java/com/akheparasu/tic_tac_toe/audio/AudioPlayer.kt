package com.akheparasu.tic_tac_toe.audio

import android.content.Context
import android.media.MediaPlayer

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun playSound(resourceId: Int) {
        mediaPlayer = MediaPlayer.create(context, resourceId)
        mediaPlayer?.start()
    }

    fun stopSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }
}
