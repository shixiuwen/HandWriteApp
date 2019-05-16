package com.shixia.handswirteapp;

import android.content.Context;
import android.graphics.Bitmap;
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

    private static final int PATH_STROKE = 10;
    private int width, height;
    private Bitmap bitmap;
    private Canvas canvas;

    private float lastPointX, lastPointY;
    private boolean isDownAction = true;
    private int pathPaintSortCount = 0;

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
        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.purple));

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
//                path.moveTo(event.getX(), event.getY());
                isDownAction = true;
                lastPointX = event.getX();
                lastPointY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                isUpEvent = true;
                break;
        }
        if (isUpEvent) {
            if (fingerPoints.size() == 3) {
                pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.black));
                pathPaintSortCount += 1;
                pathPaint.setStrokeWidth(calculatePaintStrokeWidthWithSpace(fingerPoints));
                path.moveTo(lastPointX, lastPointY);
                path.quadTo(fingerPoints.get(1).x, fingerPoints.get(1).y, fingerPoints.get(2).x, fingerPoints.get(2).y);
            } else if (fingerPoints.size() > 3) {
                pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.red));
                pathPaintSortCount += 1;
                pathPaint.setStrokeWidth(calculatePaintStrokeWidthWithSpace(fingerPoints));
                path.moveTo(lastPointX, lastPointY);
                path.cubicTo(fingerPoints.get(1).x, fingerPoints.get(1).y, fingerPoints.get(2).x, fingerPoints.get(2).y
                        , fingerPoints.get(3).x, fingerPoints.get(3).y);
            }
            calculatePaintStrokeWidthWithSpace(fingerPoints);
            canvas.drawPath(path, pathPaint);
            invalidate();
            fingerPoints.clear();
            path.reset();
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
//                path.cubicTo(fingerPoints.get(1).x, fingerPoints.get(1).y
//                        , fingerPoints.get(2).x, fingerPoints.get(2).y
//                        , Ox, Oy);
                path.moveTo(lastPointX, lastPointY);
                path.cubicTo(fingerPoints.get(1).x, fingerPoints.get(1).y
                        , fingerPoints.get(2).x, fingerPoints.get(2).y
                        , Ox, Oy);
                lastPointX = Ox;
                lastPointY = Oy;
                switch (tempPoints.size() % 5) {
                    case 0:
                        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                        break;
                    case 1:
                        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.blue));
                        break;
                    case 2:
                        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.orange));
                        break;
                    case 3:
                        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.purple));
                        break;
                    case 4:
                        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.yellow));
                        break;
                    default:
                        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.green));
                        break;
                }
                calculatePaintStrokeWidthWithSpace(fingerPoints);
                if (isDownAction) {
                    pathPaintSortCount = 0;
                    pathPaint.setStrokeWidth(calculatePaintStrokeWidthWithSpace(fingerPoints));
                    isDownAction = false;
                } else {
                    pathPaintSortCount += 1;
                    pathPaint.setStrokeWidth(calculatePaintStrokeWidthWithSpace(fingerPoints));
                }
                canvas.drawPath(path, pathPaint);
                invalidate();
                path.reset();
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

    private double tempSpace = 0;
    private int tempStroke = 0;

    private int calculatePaintStrokeWidthWithSpace(List<Points> fingerPoints) {
        double space = 0;
        for (int i = 0; i < fingerPoints.size() - 1; i++) {
            float xLength = Math.abs(fingerPoints.get(i).x - fingerPoints.get(i + 1).x);
            float yLength = Math.abs(fingerPoints.get(i).y - fingerPoints.get(i + 1).y);
            space += Math.sqrt(xLength * xLength + yLength * yLength);
        }

        String log ;
        if (pathPaintSortCount == 0) {
            tempStroke = PATH_STROKE;
            log = "起始触点，笔触最大";
        } else if (tempSpace > space) {    //点序变大且点间距离变长，说明手速加快，笔锋变细
            if (tempStroke > 3) {
                tempStroke -= 1;
            }
            log = "间距增大，笔触变细";
        } else if (tempSpace < space) {    //点序变大且点间距离变短，说明手速变慢，笔锋变粗
            if (tempStroke < PATH_STROKE) {
                tempStroke += 1;
            }
            log = "间距减小，笔触变粗";
        } else {
            log = "space:" + space + " tempSpace:" + tempSpace;
        }
        tempSpace = space;
        Log.e("pathStroke", log + " " + "width:" + tempStroke + " tempSpace:" + tempSpace);
        return tempStroke;
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
