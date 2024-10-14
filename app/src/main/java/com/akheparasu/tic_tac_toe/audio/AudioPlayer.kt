package com.akheparasu.tic_tac_toe.audio

import android.content.Context
import android.media.MediaPlayer
import com.akheparasu.tic_tac_toe.R
import com.akheparasu.tic_tac_toe.utils.DEFAULT_VOLUME

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var volume: Float = DEFAULT_VOLUME

    fun setVolume(vol: Float) {
        //pauseAudio()
        volume = vol
        mediaPlayer?.setVolume(volume, volume)
        //resumeAudio()
    }

    fun onPlayerTap() {
        playAudio(R.raw.player_tap)
    }

    fun onOpponentTap() {
        playAudio(R.raw.opponent_tap)
    }

    fun onGameStart() {
        playAudio(R.raw.game_start)
    }

    fun onWin() {
        playAudio(R.raw.game_win)
    }

    fun onFail() {
        playAudio(R.raw.game_fail)
    }

    fun onDraw() {
        playAudio(R.raw.game_draw)
    }

    private fun playAudio(audioResId: Int) {
        stopAudio()
        mediaPlayer = MediaPlayer.create(context, audioResId)
        mediaPlayer?.setVolume(volume, volume)
        mediaPlayer?.start()
    }

    private fun stopAudio() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
    }

    private fun resumeAudio() {
        mediaPlayer?.start()
    }
}
