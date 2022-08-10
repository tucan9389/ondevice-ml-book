package com.example.imagelabellingsample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val img: ImageView = findViewById(R.id.imageToLabel)
        // assets 폴더의 이미지 파일과 확장자
        val fileName = "figure4-1.jpg"
        // assets 폴더에서 비트맵 이미지 가져오기
        val bitmap: Bitmap? = assetsToBitmap(fileName)
        bitmap?.apply {
            img.setImageBitmap(this)
        }
        val txtOutput : TextView = findViewById(R.id.txtOutput)


        val btn: Button = findViewById(R.id.btnTest)
        btn.setOnClickListener {
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap!!, 0)
            var outputText = ""
            labeler.process(image)
                    .addOnSuccessListener { labels ->
                        // 테스크가 성공했을 때의 처리
                        for (label in labels) {
                            val text = label.text
                            val confidence = label.confidence
                            outputText += "$text : $confidence\n"
                            //val index = label.index
                        }
                        txtOutput.text = outputText
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
