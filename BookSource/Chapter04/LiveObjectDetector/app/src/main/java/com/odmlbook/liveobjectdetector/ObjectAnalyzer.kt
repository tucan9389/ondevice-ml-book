package com.odmlbook.liveobjectdetector

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

public class ObjectAnalyzer(graphicOverlay: GraphicOverlay) : ImageAnalysis.Analyzer {
    val options =
            ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableMultipleObjects()
                    .enableClassification()
                    .build()
    val objectDetector = ObjectDetection.getClient(options)
    val overlay = graphicOverlay
    private val lensFacing = CameraSelector.LENS_FACING_BACK

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        if (rotationDegrees == 0 || rotationDegrees == 180) {
            overlay.setImageSourceInfo(
                imageProxy.width, imageProxy.height, isImageFlipped
            )
        } else {
            overlay.setImageSourceInfo(
                imageProxy.height, imageProxy.width, isImageFlipped
            )
        }
        val frame = InputImage.fromMediaImage(
            imageProxy.image!!,
            imageProxy.imageInfo.rotationDegrees
        )
        objectDetector.process(frame)
                .addOnSuccessListener { detectedObjects ->
                    // 테스크가 성공적으로 끝났을 때 처리
                    overlay.clear()
                    for (detectedObject in detectedObjects){
                        val objGraphic = ObjectGraphic(this.overlay, detectedObject)
                        this.overlay.add(objGraphic)
                    }
                    this.overlay.postInvalidate()

                }

                .addOnFailureListener { e ->
                    // 테스크가 실패했을 때 예외처리
                    // ...
                }
                .addOnCompleteListener {
                    imageProxy.close()

                }


    }

}

