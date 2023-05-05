package com.example.fd

import android.content.Context
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val img: ImageView = findViewById(R.id.imageFace)
        // assets 폴더의 이미지 파일과 확장자
        val fileName = "face-test.jpg"

        // assets 폴더에서 비트맵 이미지 가져오기
        val bitmap: Bitmap? = assetsToBitmap(fileName)
        bitmap?.apply{
            img.setImageBitmap(this)
        }

        val btn: Button = findViewById(R.id.btnTest)
        btn.setOnClickListener {
            val highAccuracyOpts = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()

            val detector = FaceDetection.getClient(highAccuracyOpts)
            val image = InputImage.fromBitmap(bitmap!!, 0)
            val result = detector.process(image)
                .addOnSuccessListener { faces ->
                    // 성공적으로 테스크가 끝나면
                    // ...
                    bitmap?.apply{
                        img.setImageBitmap(drawWithRectangle(faces))
                    }
                }
                .addOnFailureListener { e ->
                    // 테스크가 실패했을 때의 예외처리
                    // ...
                }
        }
    }
}
// assets 폴더에서 비트맵 이미지를 가져오기 위한 핼퍼 함수
fun Context.assetsToBitmap(fileName: String): Bitmap?{
    return try {
        with(assets.open(fileName)){
            BitmapFactory.decodeStream(this)
        }
    } catch (e: IOException) { null }
}


fun Bitmap.drawWithRectangle(faces: List<Face>):Bitmap?{
    val bitmap = copy(config, true)
    val canvas = Canvas(bitmap)
    for (face in faces){
        val bounds = face.boundingBox
        Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 4.0f
            isAntiAlias = true
            // 캔버스에 사각형 그리기
            canvas.drawRect(
                bounds,
                this
            )
        }
    }
    return bitmap
}