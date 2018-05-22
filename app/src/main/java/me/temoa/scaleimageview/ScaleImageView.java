package me.temoa.scaleimageview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by lai
 * on 2018/5/17.
 */

@SuppressLint("AppCompatCustomView")
public class ScaleImageView extends ImageView implements
        ScaleGestureDetector.OnScaleGestureListener,
        View.OnTouchListener, ViewTreeObserver.OnGlobalLayoutListener {

    private boolean mOnce;

    private float mInitScale;
    private float mMidScale;
    private float mMaxScale;
    private Matrix mMatrix;
    private boolean mIsScaling;
    private ScaleGestureDetector mScaleGestureDetector;

    private int mLastPointerCount;
    private float mLastPointerX;
    private float mLastPointerY;

    private float mTouchSlop;

    private boolean mIsCanDrag;
    private boolean mIsCheckLeftAndRight;
    private boolean mIsCheckTopAndBottom;

    private GestureDetector mGestureDetector;

    private class SlowlyScaleRunnable implements Runnable {

        private float targetScale; // 目标缩放值
        private float scaleCenterX;
        private float scaleCenterY;

        private final float BIGGER = 1.07F; // 放大梯度值
        private final float SMALLER = 0.93F; // 缩小梯度值
        private float tempScale; // 缩放梯度

        SlowlyScaleRunnable(float targetScale, float centerX, float centerY) {
            this.targetScale = targetScale;
            this.scaleCenterX = centerX;
            this.scaleCenterY = centerY;
            // 判断是放大还是缩小
            if (getScale() < targetScale) {
                tempScale = BIGGER;
            }
            if (getScale() > targetScale) {
                tempScale = SMALLER;
            }
        }

        @Override
        public void run() {
            mMatrix.postScale(tempScale, tempScale, scaleCenterX, scaleCenterY);
            checkBorderAndCenterWhenScale();
            setImageMatrix(mMatrix);
            float currentScale = getScale();
            if ((tempScale > 1.0F && currentScale < targetScale)
                    || (tempScale < 1.0F && currentScale > targetScale)) {
                postDelayed(this, 16);
            } else {
                float scale = targetScale / currentScale;
                mMatrix.postScale(scale, scale, scaleCenterX, scaleCenterY);
                checkBorderAndCenterWhenScale();
                setImageMatrix(mMatrix);
                mIsScaling = false;
            }
        }
    }

    public ScaleImageView(Context context) {
        this(context, null);
    }

    public ScaleImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScaleImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mMatrix = new Matrix();
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setScaleType(ScaleType.MATRIX);

        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        setOnTouchListener(this);
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mIsScaling) {
                    return true;
                }
                float x = e.getX();
                float y = e.getY();
                if (getScale() < mMidScale) {
                    postDelayed(new SlowlyScaleRunnable(mMidScale, x, y), 16);
                    mIsScaling = true;
                } else {
                    postDelayed(new SlowlyScaleRunnable(mInitScale, x, y), 16);
                    mIsScaling = true;
                }
                return true;
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void onGlobalLayout() {
        if (mOnce) return;

        int width = getWidth();
        int height = getHeight();
        Drawable drawable = getDrawable();
        if (drawable == null) return;
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        float scale = 1.0F;
        if (width > drawableWidth && height < drawableHeight) {
            scale = 1.0F * height / drawableHeight;
        }

        if (width < drawableWidth && height > drawableHeight) {
            scale = 1.0F * width / drawableWidth;
        }

        if ((width < drawableWidth && height < drawableHeight)
                || (width > drawableWidth && height > drawableHeight)) {
            scale = Math.min(1.0F * width / drawableWidth, 1.0F * height / drawableHeight);
        }

        mInitScale = scale;
        mMidScale = 2 * mInitScale;
        mMaxScale = 3 * mInitScale;

        int dx = width / 2 - drawableWidth / 2;
        int dy = height / 2 - drawableHeight / 2;
        mMatrix.postTranslate(dx, dy);
        mMatrix.postScale(mInitScale, mInitScale, width / 2, height / 2);
        setImageMatrix(mMatrix);

        mOnce = true;
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = getScale();
        float scaleFactor = detector.getScaleFactor(); // 缩放因子 大于1.0为放大，小于1.0为缩小

        if (getDrawable() == null) return true;

        if ((scale < mMaxScale && scaleFactor > 1.0F)
                || (scale > mInitScale && scaleFactor < 1.0F)) {
            if (scale * scaleFactor > mMaxScale) {
                scaleFactor = mMaxScale / scale;
            }
            if (scale * scaleFactor < mInitScale) {
                scaleFactor = mInitScale / scale;
            }

            mMatrix.postScale(
                    scaleFactor, scaleFactor,
                    detector.getFocusX(), detector.getFocusY());
            checkBorderAndCenterWhenScale();
            setImageMatrix(mMatrix);
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    // --------------------------------------------------------------------------------------------

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) return true;

        boolean onTouchEvent = mScaleGestureDetector.onTouchEvent(event);

        float pointerX = 0;
        float pointerY = 0;

        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            pointerX += event.getX(i);
            pointerY += event.getY(i);
        }
        pointerX /= pointerCount;
        pointerY /= pointerCount;
        if (mLastPointerCount != pointerCount) {
            mIsCanDrag = false;
            mLastPointerX = pointerX;
            mLastPointerY = pointerY;
        }
        mLastPointerCount = pointerCount;

        RectF rect = getMatrixRectF();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (rect.width() - getWidth() > 0.01 || rect.height() - getHeight() > 0.01) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = pointerX - mLastPointerX;
                float dy = pointerY - mLastPointerY;
                if (!mIsCanDrag) mIsCanDrag = isMoveAction(dx, dy);
                if (mIsCanDrag) {
                    if (getDrawable() != null) {
                        if (getMatrixRectF().left == 0 && dx > 0) {
                            getParent().requestDisallowInterceptTouchEvent(false);
                            return false;
                        }
                        if (getMatrixRectF().right == getWidth() && dx < 0) {
                            getParent().requestDisallowInterceptTouchEvent(false);
                            return false;
                        }

                        mIsCheckLeftAndRight = mIsCheckTopAndBottom = true;
                        if (rect.width() < getWidth()) {
                            mIsCheckLeftAndRight = false;
                            dx = 0;
                        }
                        if (rect.height() < getHeight()) {
                            mIsCheckTopAndBottom = false;
                            dy = 0;
                        }

                        mMatrix.postTranslate(dx, dy);
                        checkBorderWhenTranslate();
                        setImageMatrix(mMatrix);
                    }
                }
                mLastPointerX = pointerX;
                mLastPointerY = pointerY;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mLastPointerCount = 0;
                break;
        }
        return true;
    }

    // --------------------------------------------------------------------------------------------

    private float getScale() {
        float[] values = new float[9];
        mMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    private RectF getMatrixRectF() {
        Matrix matrix = mMatrix;
        RectF rectF = new RectF();
        Drawable drawable = getDrawable();
        if (drawable != null) {
            rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }
        return rectF;
    }

    /**
     * 当缩放的中心点不在屏幕中心点时，缩放可能会引起图片的位置发生变化
     * 图片宽高大于屏幕时出现白边，小于屏幕时图片布局中
     * <p>
     * 所以需要对图片显示显示的范围进行控制
     */
    private void checkBorderAndCenterWhenScale() {
        RectF rect = getMatrixRectF();
        float delatX = 0;
        float delatY = 0;

        int width = getWidth();
        int height = getHeight();

        if (rect.width() >= width) {
            if (rect.left > 0) {
                delatX = -rect.left;
            }
            if (rect.right < width) {
                delatX = width - rect.right;
            }
        }
        if (rect.height() >= height) {
            if (rect.top > 0) {
                delatY = -rect.top;
            }
            if (rect.bottom < height) {
                delatY = height - rect.bottom;
            }
        }

        if (rect.width() < width) {
            delatX = width / 2F - rect.right + rect.width() / 2F;
        }
        if (rect.height() < height) {
            delatY = height / 2F - rect.bottom + rect.height() / 2F;
        }
        mMatrix.postTranslate(delatX, delatY);
    }

    private boolean isMoveAction(float dx, float dy) {
        return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
    }

    private void checkBorderWhenTranslate() {
        RectF rect = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        if (rect.top > 0 && mIsCheckTopAndBottom) {
            deltaY = -rect.top;
        }
        if (rect.bottom < height && mIsCheckTopAndBottom) {
            deltaY = height - rect.bottom;
        }
        if (rect.left > 0 && mIsCheckLeftAndRight) {
            deltaX = -rect.left;
        }
        if (rect.right < width && mIsCheckLeftAndRight) {
            deltaX = width - rect.right;
        }

        mMatrix.postTranslate(deltaX, deltaY);
    }
}
