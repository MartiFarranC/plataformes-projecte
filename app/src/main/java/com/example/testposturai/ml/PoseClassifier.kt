package com.example.testposturai.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class PoseClassifier(context: Context, modelPath: String) {
    private var tflite: Interpreter

    init {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFileDescriptor.startOffset, assetFileDescriptor.declaredLength)
        tflite = Interpreter(modelBuffer)
    }

    fun classify(bitmap: Bitmap): Pair<Int, Int> {
        val resized = Bitmap.createScaledBitmap(bitmap, 160, 160, true)
        val input = ByteBuffer.allocateDirect(1 * 160 * 160 * 3).apply { order(ByteOrder.nativeOrder()) }
        val intValues = IntArray(160 * 160)
        resized.getPixels(intValues, 0, 160, 0, 0, 160, 160)

        for (pixel in intValues) {
            input.put(((pixel shr 16) and 0xFF).toByte())
            input.put(((pixel shr 8) and 0xFF).toByte())
            input.put((pixel and 0xFF).toByte())
        }

        val output = Array(1) { ByteArray(2) }
        tflite.run(input, output)

        return Pair(output[0][0].toInt() and 0xFF, output[0][1].toInt() and 0xFF)
    }
}