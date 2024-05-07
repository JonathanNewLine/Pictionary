package com.example.pictionary;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;

import androidx.annotation.ColorInt;

import java.util.ArrayList;
import java.util.List;

public class PathShape {
    private final Path path = new Path();
    private @ColorInt int color;

    public PathShape(float startX, float startY, int color) {
        this.color = color;
        path.moveTo(startX, startY);
    }

    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(color);
        canvas.drawPath(path, paint);
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void updatePoint(float xPathUpdate, float yPathUpdate) {
        path.lineTo(xPathUpdate, yPathUpdate);
    }
}
