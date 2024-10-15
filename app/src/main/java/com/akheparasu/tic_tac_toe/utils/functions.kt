package com.akheparasu.tic_tac_toe.utils

import android.os.Handler
import android.os.Looper

fun retryTask(performTask: () -> Boolean, intervalMS: Long = 1000, maxTries: Int = 3): Unit {
    val handler = Handler(Looper.getMainLooper())
    var counter = 0
    val runnable = object : Runnable {
        override fun run() {
            if (counter < maxTries) {
                val taskSucceeded = performTask()
                if (taskSucceeded) {
                    handler.removeCallbacks(this)
                } else {
                    counter++
                    handler.postDelayed(this, intervalMS)
                }
            } else {
                handler.removeCallbacks(this)
            }
        }
    }
    handler.post(runnable)
}