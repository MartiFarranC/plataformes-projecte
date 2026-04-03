package com.example.testposturai

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.FileInputStream
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {

    private lateinit var tflite: Interpreter
    private lateinit var txtResultat: TextView
    private var darreraAlerta: Long = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Disseny ràpid: càmera a dalt, text a baix
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }
        val viewFinder = androidx.camera.view.PreviewView(this)
        txtResultat = TextView(this).apply {
            textSize = 24f
            setPadding(50, 50, 50, 50)
            text = "Iniciant càmera..."
        }
        layout.addView(viewFinder, android.widget.LinearLayout.LayoutParams(matchParent, 0, 1f))
        layout.addView(txtResultat)
        setContentView(layout)

        // 1. Carregar el model TFLite des de la carpeta Assets
        try {
            val modelFile = assets.openFd("tflite_learn_901615_40.tflite")
            val inputStream = FileInputStream(modelFile.fileDescriptor)
            val modelBuffer = inputStream.channel.map(FileChannel.MapMode.READ_ONLY, modelFile.startOffset, modelFile.declaredLength)
            tflite = Interpreter(modelBuffer)

            startCamera(viewFinder)
        } catch (e: Exception) {
            txtResultat.text = "Error carregant model: ${e.message}"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startCamera(viewFinder: androidx.camera.view.PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                processImage(imageProxy)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
            } catch (e: Exception) {
                txtResultat.text = "Error càmera: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun processImage(image: ImageProxy) {
        // El model espera 160x160 (com vas configurar a Edge Impulse)
        val bitmap = image.toBitmap() // Funció d'extensió a sota
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 160, 160, true)

        // Preparar el buffer d'entrada (RGB, 160x160, 3 canals)
        val input = ByteBuffer.allocateDirect(1 * 160 * 160 * 3).apply { order(ByteOrder.nativeOrder()) }
        val intValues = IntArray(160 * 160)
        resizedBitmap.getPixels(intValues, 0, 160, 0, 0, 160, 160)

        for (pixelValue in intValues) {
            input.put(((pixelValue shr 16) and 0xFF).toByte())
            input.put(((pixelValue shr 8) and 0xFF).toByte())
            input.put((pixelValue and 0xFF).toByte())
        }

        // Preparar la sortida (Correcte vs Incorrecte)
        val output = Array(1) { ByteArray(2) } // El model INT8 sol retornar Byte
        tflite.run(input, output)

        val valors = output[0]
        runOnUiThread {
            // El valor 0 sol ser 'Correcte' i el 1 'Incorrecte' (depèn de l'ordre alfabètic a EI)
            // Si veus que els labels estan girats, canvia el 0 pel 1
            val probCorrecte = valors[0].toInt() and 0xFF
            val probIncorrecte = valors[1].toInt() and 0xFF

            if (probIncorrecte > probCorrecte) {
                txtResultat.text = "POSTURA INCORRECTE ($probIncorrecte)"
                txtResultat.setBackgroundColor(android.graphics.Color.RED)
                ferVibrar()
            } else {
                txtResultat.text = "POSTURA CORRECTE ($probCorrecte)"
                txtResultat.setBackgroundColor(android.graphics.Color.GREEN)
            }
        }
        image.close()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ferVibrar() {
        val ara = System.currentTimeMillis()
        if (ara - darreraAlerta > 2000) { // Només vibrar cada 2 segons
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            darreraAlerta = ara
        }
    }

    private val matchParent = android.view.ViewGroup.LayoutParams.MATCH_PARENT
}