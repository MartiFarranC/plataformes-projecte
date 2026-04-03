package com.example.testposturai.sensors

import android.hardware.*
import kotlin.math.roundToInt

class AngleProvider(private val onAngleChanged: (Int) -> Unit) : SensorEventListener {

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val matrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(matrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(matrix, orientation)

            var degrees = Math.toDegrees(orientation[1].toDouble()).roundToInt()
            // Lògica per 360 si vols fer servir atan2 com hem parlat abans
            val gravY = matrix[7]
            val gravZ = matrix[8]
            var fullDegrees = Math.toDegrees(Math.atan2(gravY.toDouble(), gravZ.toDouble())).roundToInt()
            if (fullDegrees < 0) fullDegrees += 360

            onAngleChanged(fullDegrees)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}