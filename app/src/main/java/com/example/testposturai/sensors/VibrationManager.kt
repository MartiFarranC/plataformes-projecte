package com.example.testposturai.sensors

import android.content.Context
import android.os.*

class VibrationManager(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var isVibrating = false
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private val runnable = object : Runnable {
        override fun run() {
            if (isVibrating) {
                triggerVibration()
                handler.postDelayed(this, 5000)
            }
        }
    }

    fun startAlert() {
        if (!isVibrating) {
            isVibrating = true
            handler.post(runnable)
        }
    }

    fun stopAlert() {
        isVibrating = false
        handler.removeCallbacks(runnable)
    }

    private fun triggerVibration() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 500, 100, 500)
                val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                vibrator.vibrate(500)
            }
        }
    }
}