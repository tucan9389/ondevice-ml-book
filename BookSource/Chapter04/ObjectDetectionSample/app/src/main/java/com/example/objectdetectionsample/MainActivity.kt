package com.example.objectdetectionsample

import android.content.Context
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //val croppedImage: ImageView = findViewById(R.id.croppedImage)
        val img: ImageView = findViewById(R.id.imageToLabel)
        // assets 폴더의 이미지 파일과 확장자
        val fileName = "bird.jpg"
        // assets 폴더에서 비트맵 이미지 가져오기
        val bitmap: Bitmap? = assetsToBitmap(fileName)
        bitmap?.apply {
            img.setImageBitmap(this)
        }

        val options =
                ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .build()


        val txtOutput : TextView = findViewById(R.id.txtOutput)
        val btn: Button = findViewById(R.id.btnTest)

        btn.setOnClickListener {
            val objectDetector = ObjectDetection.getClient(options)
            var image = InputImage.fromBitmap(bitmap!!, 0)
            txtOutput.text = ""
            objectDetector.process(image)
                    .addOnSuccessListener { detectedObjects ->
                        // 성공적으로 테스크가 끝났을 때 처리
                        getLabels(bitmap, detectedObjects, txtOutput)
                        bitmap?.apply{
                            img.setImageBitmap(drawWithRectangle(detectedObjects))
                        }

                    }
                    .addOnFailureListener { e ->
                        // 테스크가 실패했을 때의 예외처리
                        // ...
                    }


        }


    }
}

fun getLabels(bitmap: Bitmap, objects: List<DetectedObject>, txtOutput: TextView){
    val labeler =
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    for(obj in objects) {
        val bounds = obj.boundingBox
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            bounds.left,
            bounds.top,
            bounds.width(),
            bounds.height()
        )
        //croppedImage.setImageBitmap(croppedBitmap)
        var image = InputImage.fromBitmap(croppedBitmap!!, 0)
        labeler.process(image)
            .addOnSuccessListener { labels ->
                // 성공적으로 테스크가 끝났을 때 처리
                var labelText = ""
                if(labels.count()>0) {
                    labelText = txtOutput.text.toString()
                    for (thisLabel in labels){
                        labelText += thisLabel.text + " , "
                    }
                    labelText += "\n"
                } else {
                    labelText = "Not found." + "\n"
                }
                txtOutput.text = labelText.toString()
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

fun Bitmap.drawWithRectangle(objects: List<DetectedObject>):Bitmap?{
    val bitmap = copy(config, true)
    val canvas = Canvas(bitmap)
    var thisLabel = 0
    for (obj in objects){
        thisLabel++
        val bounds = obj.boundingBox
        Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            textSize = 32.0f
            strokeWidth = 4.0f
            isAntiAlias = true
            // 캔버스에 사각형 그리기
            canvas.drawRect(
                    bounds,
                    this
            )
            canvas.drawText(thisLabel.toString(), bounds.left.toFloat(), bounds.top.toFloat(), this )
        }

    }
    return bitmap
}