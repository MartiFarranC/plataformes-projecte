package com.example.testposturai.ui

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.testposturai.ml.PoseClassifier
import com.example.testposturai.sensors.AngleProvider
import com.example.testposturai.sensors.VibrationManager

class MainActivity : AppCompatActivity() {

    // Instàncies de les nostres classes modulars
    private lateinit var classifier: PoseClassifier
    private lateinit var vibrationManager: VibrationManager
    private lateinit var angleProvider: AngleProvider
    private lateinit var sensorManager: SensorManager

    // Elements de la UI
    private lateinit var txtResultat: TextView
    private lateinit var txtAngle: TextView
    private lateinit var viewFinder: PreviewView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configurar la interfície (UI)
        configurarUI()

        // 2. Inicialitzar components modulars
        classifier = PoseClassifier(this, "tflite_learn_901615_40.tflite")
        vibrationManager = VibrationManager(this)

        // Inicialitzem l'AngleProvider amb una "callback" (el que ha de fer quan l'angle canviï)
        angleProvider = AngleProvider { graus ->
            actualitzarEstatPerAngle(graus)
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // 3. Posar en marxa la càmera
        startCamera()
    }

    private fun configurarUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        txtAngle = TextView(this).apply {
            textSize = 20f
            setPadding(30, 100, 30, 20)
            text = "Angle: --°"
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(Color.LTGRAY)
        }

        viewFinder = PreviewView(this)

        txtResultat = TextView(this).apply {
            textSize = 24f
            setPadding(50, 50, 50, 50)
            text = "Analitzant postura..."
        }

        layout.addView(txtAngle)
        layout.addView(viewFinder, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        layout.addView(txtResultat)
        setContentView(layout)
    }

    private fun actualitzarEstatPerAngle(graus: Int) {
        runOnUiThread {
            txtAngle.text = "Angle real: ${graus}°"

            // Lògica de control segons Theme I-1
            if (graus < 60 || graus > 100) {
                txtAngle.setTextColor(Color.RED)
                vibrationManager.startAlert() // Cridem al manager modular
            } else {
                txtAngle.setTextColor(Color.BLACK)
                vibrationManager.stopAlert()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // Connectem l'anàlisi d'imatge amb el nostre PoseClassifier
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                val bitmap = imageProxy.toBitmap()
                val (probCorrecte, probIncorrecte) = classifier.classify(bitmap)

                runOnUiThread {
                    if (probIncorrecte > probCorrecte) {
                        txtResultat.text = "POSTURA INCORRECTE ($probIncorrecte)"
                        txtResultat.setBackgroundColor(Color.RED)
                    } else {
                        txtResultat.text = "POSTURA CORRECTE ($probCorrecte)"
                        txtResultat.setBackgroundColor(Color.GREEN)
                    }
                }
                imageProxy.close()
            }

            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onResume() {
        super.onResume()
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(angleProvider, rotationSensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(angleProvider)
        vibrationManager.stopAlert() // Seguretat: aturar vibració si sortim de l'app
    }
}