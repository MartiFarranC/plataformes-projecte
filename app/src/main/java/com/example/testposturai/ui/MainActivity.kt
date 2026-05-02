package com.example.testposturai.ui

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.testposturai.ml.PoseClassifier
import com.example.testposturai.sensors.AngleProvider
import com.example.testposturai.sensors.VibrationManager

class MainActivity : AppCompatActivity() {

    private lateinit var classifier: PoseClassifier
    private lateinit var vibrationManager: VibrationManager
    private lateinit var angleProvider: AngleProvider
    private lateinit var sensorManager: SensorManager

    private lateinit var txtResultat: TextView
    private lateinit var txtAngle: TextView
    private lateinit var viewFinder: PreviewView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configurarUI()

        classifier = PoseClassifier(this, "tflite_learn_901615_40.tflite")
        vibrationManager = VibrationManager(this)

        angleProvider = AngleProvider { graus -> actualitzarEstatPerAngle(graus) }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        startCamera()
    }

    private fun configurarUI() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        UiKit.styleScreen(layout)

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val btnBack = Button(this).apply {
            text = "Tornar"
            setOnClickListener { finish() }
        }
        UiKit.styleSecondaryButton(btnBack)

        val title = TextView(this).apply {
            text = "Analisi postural"
            setPadding(UiKit.dp(this@MainActivity, 12), 0, 0, 0)
        }
        UiKit.styleTitle(title)
        title.textSize = 20f

        topBar.addView(btnBack, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.35f))
        topBar.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.65f))

        val statsCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        UiKit.styleCard(statsCard)

        txtAngle = TextView(this).apply {
            text = "Angle: --"
            textSize = 17f
            setTextColor(UiKit.colorPrimaryText)
        }

        txtResultat = TextView(this).apply {
            text = "Analitzant postura..."
            textSize = 18f
            setPadding(0, UiKit.dp(this@MainActivity, 10), 0, 0)
            setTextColor(UiKit.colorSecondaryText)
        }

        statsCard.addView(txtAngle)
        statsCard.addView(txtResultat)

        viewFinder = PreviewView(this).apply {
            background = UiKit.roundedDrawable(Color.WHITE, UiKit.colorBorder, 1, 16f)
            elevation = UiKit.dp(this@MainActivity, 2).toFloat()
        }

        layout.addView(topBar)
        layout.addView(statsCard)
        (statsCard.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 14)
        layout.addView(viewFinder, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        (viewFinder.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 14)

        setContentView(layout)
    }

    private fun actualitzarEstatPerAngle(graus: Int) {
        runOnUiThread {
            txtAngle.text = "Angle real: ${graus}"

            if (graus < 60 || graus > 100) {
                txtAngle.setTextColor(UiKit.colorDanger)
                vibrationManager.startAlert()
            } else {
                txtAngle.setTextColor(UiKit.colorSuccess)
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

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                val bitmap = imageProxy.toBitmap()
                val (probCorrecte, probIncorrecte) = classifier.classify(bitmap)

                runOnUiThread {
                    if (probIncorrecte > probCorrecte) {
                        txtResultat.text = "Postura incorrecta (${probIncorrecte})"
                        txtResultat.setTextColor(UiKit.colorDanger)
                    } else {
                        txtResultat.text = "Postura correcta (${probCorrecte})"
                        txtResultat.setTextColor(UiKit.colorSuccess)
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
        vibrationManager.stopAlert()
    }
}
