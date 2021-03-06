package com.shixia.handswirteapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.List;

public class HandsWriteView extends View implements View.OnTouchListener {

    private final int maximumFlingVelocity;
    private List<Points> fingerPoints = new ArrayList<>();
    private List<Points> tempPoints = new ArrayList<>();

    private Paint pointPaint;
    private Paint pathPaint;
    private Path path;

    private static final int PATH_STROKE = 10;
    private int width, height;
    private Bitmap bitmap;
    private Canvas canvas;

    private float lastPointX, lastPointY;
    private boolean isDownAction = true;
    private int pathPaintSortCount = 0;

    private int tempVelocity = 0;
    private int tempStroke = 0;

    private boolean isRealPenStyle = true;

    public HandsWriteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        setLayerType(LAYER_TYPE_SOFTWARE, null);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        pointPaint.setStrokeCap(Paint.Cap.ROUND);
        pointPaint.setStrokeWidth(4);
        pointPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));

        pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeWidth(PATH_STROKE);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        pathPaint.setPathEffect(new CornerPathEffect(200));
        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.purple));

        path = new Path();

        setOnTouchListener(this);
        maximumFlingVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }

    private VelocityTracker mVelocityTracker;
    private float p1x = -1, p1y = -1;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.e("onTouch", event.getAction() + "");
        fingerPoints.add(new Points(event.getX(), event.getY()));
        tempPoints.add(new Points(event.getX(), event.getY()));
        acquireVelocityTracker(event);
        final VelocityTracker verTracker = mVelocityTracker;
        boolean isUpEvent = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDownAction = true;
                lastPointX = event.getX();
                lastPointY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                //求伪瞬时速度
                verTracker.computeCurrentVelocity(1000, maximumFlingVelocity);
                break;
            case MotionEvent.ACTION_UP:
                isUpEvent = true;
                releaseVelocityTracker();
                break;
            case MotionEvent.ACTION_CANCEL:
                releaseVelocityTracker();
                break;
        }

        while (fingerPoints.size() > 3) {
            fingerPoints.remove(0);
        }
        if (fingerPoints.size() == 3) {
            Log.e("fingerPoints：", fingerPoints.size() + " action:" + event.getAction());
            //1.斜率计算
            float a = (fingerPoints.get(2).y - fingerPoints.get(0).y) / (fingerPoints.get(2).x - fingerPoints.get(0).x);
            //2.方程 y = ax + b 中的常数b计算，确定切线方程 y = ax + b
            float b = fingerPoints.get(1).y - a * fingerPoints.get(1).x;
            //3.
            float p2x, p2y;
            //如果斜率大于1，通过y点确定x点，否则相反
            float rate = 0.25F;
            p2x = fingerPoints.get(1).x - (fingerPoints.get(1).x - fingerPoints.get(0).x) * rate;
            p2y = fingerPoints.get(1).y - (fingerPoints.get(1).y - fingerPoints.get(0).y) * rate;
//            p2y = a * p2x + b;
            path.moveTo(lastPointX, lastPointY);
            if (p1x == -1 || p1y == -1) {//最开始的三个点
                p1x = Math.min(fingerPoints.get(0).x, p2x) + Math.abs(fingerPoints.get(0).x - p2x) / 2;
                p1y = a * p1x + b;
            }
            path.cubicTo(p1x, p1y, p2x, p2y, fingerPoints.get(1).x, fingerPoints.get(1).y);
            lastPointX = fingerPoints.get(1).x;
            lastPointY = fingerPoints.get(1).y;
            //4.同理在后面两个点上添加控制点
            p1x = fingerPoints.get(1).x + (fingerPoints.get(2).x - fingerPoints.get(0).x) * rate;
            p1y = fingerPoints.get(1).y + (fingerPoints.get(2).y - fingerPoints.get(0).y) * rate;
//            p1y = a * p1x + b;
//            switch (tempPoints.size() % 5) {
//                case 0:
//                    pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
//                    break;
//                case 1:
//                    pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.blue));
//                    break;
//                case 2:
//                    pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.orange));
//                    break;
//                case 3:
//                    pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.purple));
//                    break;
//                case 4:
//                    pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.yellow));
//                    break;
//                default:
//                    pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.green));
//                    break;
//            }
            if (isDownAction) {
                pathPaintSortCount = 0;
                pathPaint.setStrokeWidth(calculatePaintStrokeWidthWithVelocity());
                isDownAction = false;
            } else {
                pathPaintSortCount += 1;
                pathPaint.setStrokeWidth(calculatePaintStrokeWidthWithVelocity());
            }
            canvas.drawPath(path, pathPaint);
            invalidate();
            path.reset();
            fingerPoints.remove(0);
        }
        if (isUpEvent) {
            p1x = p1y = -1;
            fingerPoints.clear();
            path.reset();
        }
        return true;
    }

    /**
     * 是否使用真实的笔触样式
     *
     * @param isRealPenStyle
     */
    public void setRealPenStyle(boolean isRealPenStyle) {
        this.isRealPenStyle = isRealPenStyle;
    }

    private int calculatePaintStrokeWidthWithVelocity() {
        if (mVelocityTracker != null) {
            int v = (int) Math.sqrt(mVelocityTracker.getXVelocity() * mVelocityTracker.getXVelocity()
                    + mVelocityTracker.getYVelocity() * mVelocityTracker.getYVelocity());
            Log.e("velocity", v + "");
            if (pathPaintSortCount == 0) {
                tempStroke = PATH_STROKE;
            } else if (tempVelocity > v) {    //手速加快，笔锋变细
                if (tempStroke > 2) {
                    tempStroke -= 1;
                }
            } else if (tempVelocity < v) {    //手速变慢，笔锋变粗
                if (tempStroke < PATH_STROKE) {
                    tempStroke += 1;
                }
            }
            tempVelocity = v;
        }
        return isRealPenStyle ? tempStroke : PATH_STROKE;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.e("fingerPoints：", "ondraw");
//        canvas.drawPath(path, pathPaint);
        canvas.drawBitmap(bitmap, 0, 0, pointPaint);
//        for (int i = 0; i < tempPoints.size(); i++) {
//            canvas.drawPoint(tempPoints.get(i).x, tempPoints.get(i).y, pointPaint);
//        }
    }

    class Points {

        float x;
        float y;

        Points(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
    }

    /**
     * @param event 向VelocityTracker添加MotionEvent
     * @see android.view.VelocityTracker#obtain()
     * @see android.view.VelocityTracker#addMovement(MotionEvent)
     */
    private void acquireVelocityTracker(final MotionEvent event) {
        if (null == mVelocityTracker) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    /**
     * 释放VelocityTracker
     *
     * @see android.view.VelocityTracker#clear()
     * @see android.view.VelocityTracker#recycle()
     */
    private void releaseVelocityTracker() {
        if (null != mVelocityTracker) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }
}
