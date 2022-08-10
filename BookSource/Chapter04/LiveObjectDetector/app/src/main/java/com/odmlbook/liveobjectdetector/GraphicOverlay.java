package com.odmlbook.liveobjectdetector;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;
//import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;

/**
 * A view which renders a series of custom graphics to be overlayed on top of an associated preview
 * (i.e., the camera preview). The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.
 *
 * <p>Supports scaling and mirroring of the graphics relative the camera's preview properties. The
 * idea is that detection items are expressed in terms of an image size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.
 *
 * <p>Associated {@link Graphic} items should use the following methods to convert to view
 * coordinates for the graphics that are drawn:
 *
 * <ol>
 *   <li>{@link Graphic#scale(float)} adjusts the size of the supplied value from the image scale
 *       to the view scale.
 *   <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
 *       coordinate from the image's coordinate system to the view coordinate system.
 * </ol>
 */
public class GraphicOverlay extends View {
    private final Object lock = new Object();
    private final List<Graphic> graphics = new ArrayList<>();
    // Matrix for transforming from image coordinates to overlay view coordinates.
    private final Matrix transformationMatrix = new Matrix();

    private int imageWidth;
    private int imageHeight;
    // The factor of overlay View size to image size. Anything in the image coordinates need to be
    // scaled by this amount to fit with the area of overlay View.
    private float scaleFactor = 1.0f;
    // The number of horizontal pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    private float postScaleWidthOffset;
    // The number of vertical pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    private float postScaleHeightOffset;
    private boolean isImageFlipped;
    private boolean needUpdateTransformation = true;

    /**
     * 그레픽 오버레이 위에 그려질 커스텀 그래픽 오브젝트의 기본 클래스. 이 클래스를 상속하고, 그래픽 요소를 정의하기위해서는
     * {@link Graphic#draw(Canvas)} 메소드를 구현(implement)하세요. 오버레이에 인스턴스를 추가하려면
     * {@link GraphicOverlay#add(Graphic)}를 사용하십시오.
     *
     */
    public abstract static class Graphic {
        private GraphicOverlay overlay;

        public Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
        }

        /**
         * 주입된 캔버스에 그래픽을 그립니다. 그려질 그래픽에 뷰 좌표를 변환하려면 아래 메소드를 사용하여 그려야 합니다.
         *
         * <ol>
         *   <li>{@link Graphic#scale(float)}는 주어진 값을 이미지 크기에서 뷰 크기로 조정합니다.
         *   <li>{@link Graphic#translateX(float)}와 {@link Graphic#translateY(float)}는 이미지 좌표계에서
         *       뷰 좌표계로 변환합니다.
         * </ol>
         *
         * @param canvas 그릴 캔버스
         */
        public abstract void draw(Canvas canvas);

        /** 주입된 값을 이미지 스케일에서 뷰 스케일로 조정합니다. */
        public float scale(float imagePixel) {
            return imagePixel * overlay.scaleFactor;
        }

        /** 앱의 어플리케이션 컨텍스트를 반환합니다. */
        public Context getApplicationContext() {
            return overlay.getContext().getApplicationContext();
        }

        public boolean isImageFlipped() {
            return overlay.isImageFlipped;
        }

        /**
         * x 좌표를 이미지 좌표계에서 뷰 좌표계로 조정합니다.
         */
        public float translateX(float x) {
            if (overlay.isImageFlipped) {
                return overlay.getWidth() - (scale(x) - overlay.postScaleWidthOffset);
            } else {
                return scale(x) - overlay.postScaleWidthOffset;
            }
        }

        /**
         * y 좌표를 이미지 좌표계에서 뷰 좌표계로 조정합니다.
         */
        public float translateY(float y) {
            return scale(y) - overlay.postScaleHeightOffset;
        }

        /**
         * 이미지 좌표에서 오버레이 뷰 좌표로로 변환하기위해 {@link Matrix}를 반환합니다.
         */
        public Matrix getTransformationMatrix() {
            return overlay.transformationMatrix;
        }

        public void postInvalidate() {
            overlay.postInvalidate();
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        needUpdateTransformation = true);
    }

    /** 오버레이 위의 모든 그래픽들을 제거합니다. */
    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    /** 오버레이 위에 그래픽 하나를 추가합니다. */
    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
    }

    /** 오버레이의 하나의 그레픽을 제거합니다. */
    public void remove(Graphic graphic) {
        synchronized (lock) {
            graphics.remove(graphic);
        }
        postInvalidate();
    }

    /**
     *
     * 감지기(detectors)로 처리된 이미지의 소스 정보를 설정합니다. 이 정보에는 크기, 뒤집혔는지를 담고 있으며, 뒤에서 이미지
     * 좌표 변환에서 사용됩니다.
     *
     * @param imageWidth ML Kit 감지기(detectors)에 보내는 이미지 가로 길이
     * @param imageHeight ML Kit 감지기(detectors)에 보내는 이미지 세로 길이
     * @param isFlipped 이미지가 뒤집혔는지 여부. 전면 카메라에서 만들어진 이미지라면 true로 설정되어야 함
     */
    public void setImageSourceInfo(int imageWidth, int imageHeight, boolean isFlipped) {
        //Preconditions.checkState(imageWidth > 0, "image width must be positive");
        //Preconditions.checkState(imageHeight > 0, "image height must be positive");
        synchronized (lock) {
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.isImageFlipped = isFlipped;
            needUpdateTransformation = true;
        }
        postInvalidate();
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    private void updateTransformationIfNeeded() {
        if (!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0) {
            return;
        }
        float viewAspectRatio = (float) getWidth() / getHeight();
        float imageAspectRatio = (float) imageWidth / imageHeight;
        postScaleWidthOffset = 0;
        postScaleHeightOffset = 0;
        if (viewAspectRatio > imageAspectRatio) {
            // 이미지를 뷰에 올리기 위해 수직 크롭 수행
            scaleFactor = (float) getWidth() / imageWidth;
            postScaleHeightOffset = ((float) getWidth() / imageAspectRatio - getHeight()) / 2;
        } else {
            // 이미지를 뷰에 올리기 위해 평행 크롭 수행
            scaleFactor = (float) getHeight() / imageHeight;
            postScaleWidthOffset = ((float) getHeight() * imageAspectRatio - getWidth()) / 2;
        }

        transformationMatrix.reset();
        transformationMatrix.setScale(scaleFactor, scaleFactor);
        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset);

        if (isImageFlipped) {
            transformationMatrix.postScale(-1f, 1f, getWidth() / 2f, getHeight() / 2f);
        }

        needUpdateTransformation = false;
    }

    /** 관련된 그래픽 오브젝트로 오버레이 그리기 */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (lock) {
            updateTransformationIfNeeded();

            for (Graphic graphic : graphics) {
                graphic.draw(canvas);
            }
        }
    }
}

