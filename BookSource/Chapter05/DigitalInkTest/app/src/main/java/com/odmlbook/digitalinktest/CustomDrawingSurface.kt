package com.odmlbook.digitalinktest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.AttributeSet
import android.view.*
import androidx.core.content.res.ResourcesCompat
import com.google.mlkit.vision.digitalink.Ink
import com.odmlbook.digitalinktest.R

class CustomDrawingSurface @JvmOverloads constructor(context: Context?, attributeSet: AttributeSet?=null): View(context, attributeSet) {
    // 그릴 패스를 담고 있습니다
    private var path = Path()
    private val drawColor = Color.BLACK
    private val backgroundColor = Color.MAGENTA
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap
    private lateinit var frame: Rect
    private var inkBuilder = Ink.Builder()
    private lateinit var strokeBuilder: Ink.Stroke.Builder




    // 어디에 그릴지 paint를 설정합니다
    private val paint = Paint().apply {
        color = drawColor
        // 그려지는 모서리를 부드럽게 만듭니다
        isAntiAlias = true
        // 디더링(Dithering)은 기기보다 높은 정밀도의 색상을 다운 샘플링하는 방법에 영향을 줍니다
        isDither = true
        style = Paint.Style.STROKE // 기본값: FILL
        strokeJoin = Paint.Join.ROUND // 기본값: MITER
        strokeCap = Paint.Cap.ROUND // 기본값: BUTT
        strokeWidth = 4f // 기본값: Hairline-width (아주 얇음)
    }

    /**
     * 모든 픽셀을 하나하나 그리지 않습니다.
     * 이 거리보다 작게 손가락을 움직이면 그리지 않습니다. scaledTouchSlop는 사용자가 스크롤하기 전에 터치가 이동할 수 있는
     * 픽셀 거리를 번환합니다.
     */
    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    private var currentX = 0f
    private var currentY = 0f

    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f
    private var motionTouchEventT = 0L
    /**
     * 뷰 크기가 바뀔때마다 호출됩니다.
     * 뷰는 처음 크기가 없이 시작하기 떄문에 뷰가 생성되어 유효한 크기로 만들어지고 나도 호출됩니다.
     */
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)

        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)
    }

    override fun onDraw(canvas: Canvas) {
        // Draw the bitmap that has the saved path.
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
    }

    /**
     * MyCanvasView 커스텀 뷰가 클릭 엑션을 처리하지 않으므로 MyCanvasView#performClick를 호출하고 구현할 필요는
     * 없습니다.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y
        motionTouchEventT = System.currentTimeMillis()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }
        return true
    }

    /**
     * 아래 메소드는 onTouchEvent()에 정의된 대로 각기 다른 터치 이벤트들이 발생할 수 있게 설정합니다.
     * 이 방법은 when 조건 블락을 간결하게 유지하면서 동시에 각 이벤트에 발생하는 작업을 쉽게 바꿀 수 있습니다.
     * 아무것도 그리지 않을것이므로 invalidate를 호출할 필요는 없습니다.
     */
    private fun touchStart() {
        // 화면 위에 그리기 위한 용도
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        // ML Kit의 ink를 잡아두는데 사용될 stroke을 초기화하기 위한 용도
        currentX = motionTouchEventX
        currentY = motionTouchEventY
        strokeBuilder = Ink.Stroke.builder()
        strokeBuilder.addPoint(Ink.Point.create(motionTouchEventX, motionTouchEventY, motionTouchEventT))
    }

    private fun touchMove() {
        val dx = Math.abs(motionTouchEventX - currentX)
        val dy = Math.abs(motionTouchEventY - currentY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // QuadTo()는 마지막 점으로부터 이차 베이저(quadratic bezier)를 추가합니다.
            // (x1,y1) 조작점(control point)에 접근하고, (x2,y2) 점에서 끝나도록 합니다.
            // quadratic bezier: https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Quadratic_B%C3%A9zier_curves
            path.quadTo(currentX, currentY, (motionTouchEventX + currentX) / 2, (motionTouchEventY + currentY) / 2)
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            strokeBuilder.addPoint(Ink.Point.create(motionTouchEventX, motionTouchEventY, motionTouchEventT))
            // Draw the path in the extra bitmap to save it.
            // 저장하기 위해 extra bitmap에 그림을 그립니다
            extraCanvas.drawPath(path, paint)
        }
        // 이 리스너에 전달되는 다른 종류의 모션 이벤트가 많고, 뷰에서 무효화(invalidate) 하고 싶지 않으므로 
        // invalidate()는 touchMove()에서 ACTION_MOVE에 있습니다. 
        invalidate()
    }

    private fun touchUp() {
        // 그렸던 패스는 다시 그리지 않도록 리셋합니다.
        strokeBuilder.addPoint(Ink.Point.create(motionTouchEventX, motionTouchEventY, motionTouchEventT))
        inkBuilder.addStroke(strokeBuilder.build())
        path.reset()
    }

    fun getInk(): Ink{
        val ink = inkBuilder.build()
        return ink
    }

    fun clear(){
        path.reset()
        inkBuilder = Ink.builder()
        extraCanvas.drawColor(backgroundColor)
        invalidate()
    }
}
