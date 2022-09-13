package com.odmlbook.digitalinktest

import android.graphics.Canvas
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.*

class MainActivity : AppCompatActivity() {
    lateinit var txtOutput: TextView
    lateinit var customDrawingSurface: CustomDrawingSurface
    lateinit var btnClassify: Button
    lateinit var btnClear: Button
    lateinit var recognizer: DigitalInkRecognizer
    val remoteModelManager = RemoteModelManager.getInstance()
    var model: DigitalInkRecognitionModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeRecognition()
        txtOutput = findViewById(R.id.txtOutput)
        customDrawingSurface = findViewById(R.id.customDrawingSurface)
        btnClassify = findViewById(R.id.btnClassify)
        btnClassify.isEnabled = false
        btnClassify.setOnClickListener {
            val thisInk = customDrawingSurface.getInk()
            recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model!!).build() )
            recognizer.recognize(thisInk)
                .addOnSuccessListener { result: RecognitionResult ->
                    var outputString = ""
                    txtOutput.text = ""
                    for (candidate in result.candidates){
                        outputString+=candidate.text + "\n\n"
                    }
                    txtOutput.text = outputString
                }
                .addOnFailureListener { e: Exception ->
                    Log.e("Digital Ink Test", "Error during recognition: $e")
                }

        }
        btnClear = findViewById(R.id.btnClear)
        btnClear.setOnClickListener {
            customDrawingSurface.clear()
            txtOutput.text = ""
        }

    }

    fun initializeRecognition(){
        val modelIdentifier: DigitalInkRecognitionModelIdentifier? =
            DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
        // 중국어는 "zh-Hani-CN"를 사용하면 되는데, 한국어는 22.09.13 기준으로 지원하지 않습니다.
        // https://developers.google.com/android/reference/com/google/mlkit/vision/digitalink/DigitalInkRecognitionModelIdentifier#public-static-digitalinkrecognitionmodelidentifier-fromlanguagetag-string-languagetag
        model = DigitalInkRecognitionModel.builder(modelIdentifier!!).build()
        remoteModelManager.download(model!!, DownloadConditions.Builder().build()).addOnSuccessListener {
            Log.i("InkSample", "Model Downloaded")
            btnClassify.isEnabled = true
        }. addOnFailureListener {  e: Exception ->
            Log.e("InkSample", "Model failed $e")
        }
    }


}