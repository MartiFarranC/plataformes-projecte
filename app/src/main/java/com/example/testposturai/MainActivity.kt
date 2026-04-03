package com.example.testposturai

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var tflite: Interpreter
    private lateinit var txtResultat: TextView
    private lateinit var txtAngle: TextView
    private var darreraAlerta: Long = 0

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    private val handlerVibracio = android.os.Handler(android.os.Looper.getMainLooper())
    private var vibrantActiu = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialitzem el SensorManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        // Disseny: Angle a dalt, Càmera al mig, Resultat a baix
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }

        // Quadre de l'angle (A DALT)
        txtAngle = TextView(this).apply {
            textSize = 20f
            setPadding(30, 100, 30, 20)
            text = "Angle: --°"
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.LTGRAY)
        }

        val viewFinder = androidx.camera.view.PreviewView(this)

        txtResultat = TextView(this).apply {
            textSize = 24f
            setPadding(50, 50, 50, 50)
            text = "Iniciant càmera..."
        }

        layout.addView(txtAngle) // Afegim l'angle primer
        layout.addView(viewFinder, android.widget.LinearLayout.LayoutParams(matchParent, 0, 1f))
        layout.addView(txtResultat)
        setContentView(layout)

        // Carregar el model TFLite des de la carpeta Assets
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

    // --- GESTIÓ DE SENSORS PER L'ANGLE ---

    override fun onResume() {
        super.onResume()
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        gestionarAlertaVibracio(false)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // Extraiem els components de la matriu per calcular l'angle d'inclinació total
            // La gravetat en l'eix Z (rotationMatrix[8]) ens diu si la pantalla mira amunt o avall
            // La gravetat en l'eix Y (rotationMatrix[7]) ens diu la inclinació longitudinal

            val gravetatY = rotationMatrix[7]
            val gravetatZ = rotationMatrix[8]

            var angleRadiants = Math.atan2(gravetatY.toDouble(), gravetatZ.toDouble())

            var graus = Math.toDegrees(angleRadiants).roundToInt()

            if (graus < 0) {
                graus += 360
            }

            txtAngle.text = "Angle real: ${graus}°"

            if (graus < 60 || graus > 90) {
                txtAngle.setTextColor(android.graphics.Color.RED)
                gestionarAlertaVibracio(true)
            } else {
                txtAngle.setTextColor(android.graphics.Color.BLACK)
                gestionarAlertaVibracio(false)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private val runnableVibracio = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            if (vibrantActiu) {
                executarVibracioUnica()
                handlerVibracio.postDelayed(this, 5000)
            }
        }
    }

    fun gestionarAlertaVibracio(activar: Boolean) {
        if (activar && !vibrantActiu) {
            vibrantActiu = true
            handlerVibracio.post(runnableVibracio) // Inicia el cicle
        } else if (!activar) {
            vibrantActiu = false
            handlerVibracio.removeCallbacks(runnableVibracio) // Atura el cicle
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun executarVibracioUnica() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (vibrator.hasVibrator()) {
            android.util.Log.d("SENSORS", "Intentant vibració de compatibilitat...")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                val timings = longArrayOf(0, 500, 100, 500)
                val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                val efecte = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(efecte)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
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
            } else {
                txtResultat.text = "POSTURA CORRECTE ($probCorrecte)"
                txtResultat.setBackgroundColor(android.graphics.Color.GREEN)
            }
        }
        image.close()
    }


    private val matchParent = android.view.ViewGroup.LayoutParams.MATCH_PARENT
}