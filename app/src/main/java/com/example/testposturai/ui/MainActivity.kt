package com.example.testposturai.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var txtCameraStatus: TextView
    private lateinit var viewFinder: PreviewView
    private var cameraProvider: ProcessCameraProvider? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                txtCameraStatus.text = "Camera activa"
                txtCameraStatus.setTextColor(UiKit.colorSuccess)
                startCamera()
            } else {
                txtCameraStatus.text = "Permis de camera denegat"
                txtCameraStatus.setTextColor(UiKit.colorDanger)
                txtResultat.text = "No es pot iniciar l'analisi sense camera"
                txtResultat.setTextColor(UiKit.colorDanger)
                Toast.makeText(this, "Cal concedir permis de camera", Toast.LENGTH_LONG).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configurarUI()

        classifier = PoseClassifier(this, "tflite_learn_901615_40.tflite")
        vibrationManager = VibrationManager(this)

        angleProvider = AngleProvider { graus -> actualitzarEstatPerAngle(graus) }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        ensureCameraPermissionAndStart()
    }

    private fun configurarUI() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        UiKit.styleScreen(layout)

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        UiKit.styleTopBar(topBar)

        val btnBack = Button(this).apply {
            text = "Tornar"
            setOnClickListener { onBackPressedDispatcher.onBackPressed() }
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

        txtCameraStatus = TextView(this).apply {
            text = "Comprovant camera..."
        }
        UiKit.styleStatus(txtCameraStatus)

        txtAngle = TextView(this).apply {
            text = "Angle: --"
        }
        UiKit.styleBody(txtAngle)
        txtAngle.textSize = 17f

        txtResultat = TextView(this).apply {
            text = "Analitzant postura..."
            setPadding(0, UiKit.dp(this@MainActivity, 10), 0, 0)
        }
        UiKit.styleBody(txtResultat)
        txtResultat.textSize = 18f
        txtResultat.setTextColor(UiKit.colorSecondaryText)

        statsCard.addView(txtCameraStatus)
        statsCard.addView(txtAngle)
        (txtAngle.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 8)
        statsCard.addView(txtResultat)

        viewFinder = PreviewView(this).apply {
            background = UiKit.roundedDrawable(Color.WHITE, UiKit.colorBorder, 1, 20f)
            elevation = UiKit.dp(this@MainActivity, 2).toFloat()
        }

        layout.addView(topBar)
        layout.addView(statsCard)
        (statsCard.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 14)
        layout.addView(viewFinder, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        (viewFinder.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 14)

        setContentView(layout)
    }

    private fun ensureCameraPermissionAndStart() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            txtCameraStatus.text = "Camera activa"
            txtCameraStatus.setTextColor(UiKit.colorSuccess)
            startCamera()
        } else {
            txtCameraStatus.text = "Esperant permis de camera"
            txtCameraStatus.setTextColor(UiKit.colorWarning)
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
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
            cameraProvider = cameraProviderFuture.get()
            val provider = cameraProvider ?: return@addListener
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                try {
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
                } catch (_: Exception) {
                    runOnUiThread {
                        txtResultat.text = "Error processant la imatge"
                        txtResultat.setTextColor(UiKit.colorDanger)
                    }
                } finally {
                    imageProxy.close()
                }
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
            } catch (_: Exception) {
                txtCameraStatus.text = "No s'ha pogut iniciar la camera"
                txtCameraStatus.setTextColor(UiKit.colorDanger)
            }
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

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        super.onDestroy()
    }
}
