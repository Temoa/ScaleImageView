package me.temoa.scaleimageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.almeros.android.multitouch.MoveGestureDetector;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by lai
 * on 2018/5/16.
 */

public class LongImageView extends View {

    private InputStream mPhotoInputSteam;

    private Rect mRect;
    private RectF mDstRectF;
    private Matrix mScaleMatrix;

    private int mImageHeight;
    private BitmapRegionDecoder mRegionDecoder;

    private MoveGestureDetector mMoveGestureDetector;

    private static final BitmapFactory.Options sOptions = new BitmapFactory.Options();

    static {
        sOptions.inPreferredConfig = Bitmap.Config.RGB_565;
    }

    public void setInputStream(InputStream is) {
        mPhotoInputSteam = is;
    }

    public LongImageView(Context context) {
        super(context);
        init();
    }

    public LongImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LongImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mDstRectF = new RectF();
        mScaleMatrix = new Matrix();
        mMoveGestureDetector = new MoveGestureDetector(getContext(),
                new MoveGestureDetector.SimpleOnMoveGestureListener() {
                    @Override
                    public boolean onMove(MoveGestureDetector detector) {
                        PointF d = detector.getFocusDelta();
                        mRect.offset(0, (int) -d.y);
                        check();
                        postInvalidate();
                        return true;
                    }
                });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        region();
    }

    private void region() {
        if (mPhotoInputSteam == null) return;
        try {
            BitmapFactory.Options tmpOption = new BitmapFactory.Options();
            tmpOption.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(mPhotoInputSteam, null, tmpOption);
            int imageWidth = tmpOption.outWidth;
            mImageHeight = tmpOption.outHeight;

            mRegionDecoder = BitmapRegionDecoder.newInstance(mPhotoInputSteam, false);

            int height = Math.min(mImageHeight, getHeight());
            mRect = new Rect(0, 0, imageWidth, height);

            float scale = 1.0F * getWidth() / imageWidth;
            mScaleMatrix.setScale(scale, scale);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mMoveGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mRegionDecoder == null) return;
        Bitmap bitmap = mRegionDecoder.decodeRegion(mRect, sOptions);
        int x = (getWidth() - bitmap.getWidth()) / 2;
        mDstRectF.set(x, 0, (x + bitmap.getWidth()), getHeight());
        canvas.save();
//        canvas.setMatrix(mScaleMatrix);
        canvas.drawBitmap(bitmap, null, mDstRectF, null);
        canvas.restore();
        bitmap.recycle();
    }

    private void check() {
        if (mRect.bottom > mImageHeight) {
            mRect.bottom = mImageHeight;
            mRect.top = mImageHeight - getHeight();
        }

        if (mRect.top < 0) {
            mRect.top = 0;
            mRect.bottom = getHeight();
        }
    }
}
