package com.shixia.handswirteapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class HandsWriteView extends View implements View.OnTouchListener {

    private List<Points> fingerPoints = new ArrayList<>();
    private List<Points> tempPoints = new ArrayList<>();

    private Paint pointPaint;
    private Paint pathPaint;
    private Path path;

    private static final int PATH_STROKE = 5;
    private int width, height;
    private Bitmap bitmap;
    private Canvas canvas;

    private float lastPointX, lastPointY;

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
        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorPath));

        path = new Path();

        setOnTouchListener(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.e("onTouch", event.getAction() + "");
        fingerPoints.add(new Points(event.getX(), event.getY()));
        tempPoints.add(new Points(event.getX(), event.getY()));
        boolean isUpEvent = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                isUpEvent = true;
                break;
        }
        if (isUpEvent) {
            if (fingerPoints.size() == 3) {
                path.quadTo(fingerPoints.get(1).x, fingerPoints.get(1).y, fingerPoints.get(2).x, fingerPoints.get(2).y);
            } else if (fingerPoints.size() > 3) {
                path.cubicTo(fingerPoints.get(1).x, fingerPoints.get(1).y, fingerPoints.get(2).x, fingerPoints.get(2).y
                        , fingerPoints.get(3).x, fingerPoints.get(3).y);
            }
            canvas.drawPath(path, pathPaint);
            invalidate();
            fingerPoints.clear();
        }
        while (fingerPoints.size() > 5) {
            fingerPoints.remove(0);
        }
        if (fingerPoints.size() == 5) {
            Log.e("fingerPoints：", fingerPoints.size() + " action:" + event.getAction());
            //1.斜率计算
            float a = (fingerPoints.get(4).y - fingerPoints.get(2).y) / (fingerPoints.get(4).x - fingerPoints.get(2).x);
            //2.方程 y = ax + b 中的常数b计算，确定切线方程 y = ax + b
            float b = fingerPoints.get(4).y - a * fingerPoints.get(4).x;
            //3.判断当前的数据点（前面一条线的终点，后面一条线的起点）是否在前后点的矩形中，如果不是，做调整，如果是，根据x,y坐标调整改点位置，
            if ((fingerPoints.get(3).x > Math.min(fingerPoints.get(2).x, fingerPoints.get(4).x)
                    && fingerPoints.get(3).x < Math.max(fingerPoints.get(2).x, fingerPoints.get(4).x)
                    && fingerPoints.get(3).y > Math.min(fingerPoints.get(2).y, fingerPoints.get(4).y)
                    && fingerPoints.get(3).y < Math.max(fingerPoints.get(2).y, fingerPoints.get(4).y))) {
                //4.将控制点移到切线上（修改控制点），第一条曲线后一个控制点做切线垂线的交点
                float p = -1 / a;
                float q = fingerPoints.get(3).y - p * fingerPoints.get(3).x;
                //求得垂线方程 y = px + q
                //求交点
                float Ox = (b - q) / (p - a);
                float Oy = a * Ox + b;
                path.cubicTo(fingerPoints.get(1).x, fingerPoints.get(1).y
                        , fingerPoints.get(2).x, fingerPoints.get(2).y
                        , Ox, Oy);
                canvas.drawPath(path, pathPaint);
                invalidate();
                fingerPoints.remove(0);
                fingerPoints.remove(0);
                fingerPoints.remove(0);
            }
        }
        return true;
    }

    private Rect getInvalidateRec(List<Points> fingerPoints) {
        int left = 10000, top = 10000, right = 0, bottom = 0;
        for (int i = 0; i < fingerPoints.size(); i++) {
            left = Math.min((int) fingerPoints.get(i).x, left);
            top = Math.min((int) fingerPoints.get(i).y, top);
            right = Math.max((int) fingerPoints.get(i).x, right);
            bottom = Math.max((int) fingerPoints.get(i).y, bottom);
        }
        return new Rect(left - PATH_STROKE, top - PATH_STROKE, right + PATH_STROKE, bottom + PATH_STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.e("fingerPoints：", "ondraw");
//        canvas.drawPath(path, pathPaint);
        canvas.drawBitmap(bitmap, 0, 0, pointPaint);
        for (int i = 0; i < tempPoints.size(); i++) {
            canvas.drawPoint(tempPoints.get(i).x, tempPoints.get(i).y, pointPaint);
        }
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
}
