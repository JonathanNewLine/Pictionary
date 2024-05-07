package com.example.pictionary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class DrawOnView extends View {
    private final List<PathShape> strokes = new ArrayList<>();
    private static Paint paint;
    private final Client client = Client.getInstance();
    private @ColorInt int currColor = Color.BLACK;
    private PathShape currentPath;
    public final static int DEFAULT_COLOR = Color.BLACK;
    public final static int STROKE_SIZE = 10;

    public DrawOnView(Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(DEFAULT_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_SIZE);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        for (PathShape stroke : strokes) {
            stroke.draw(canvas, paint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new PathShape(x, y, currColor);
                strokes.add(currentPath);
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    currentPath.updatePoint(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (currentPath != null) {
                    currentPath.updatePoint(x, y);
                }
                client.sendBitmap(getDrawingBitmap());
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }


    public void setColor(int color) {
        paint.setColor(color);
        currColor = color;
    }

    public void undo() {
        if (!strokes.isEmpty()) {
            strokes.remove(strokes.size() - 1);
            client.sendBitmap(getDrawingBitmap());
            invalidate();
        }
    }

    public void clear() {
        strokes.clear();
        client.sendBitmap(getDrawingBitmap());
        invalidate();
    }

    public void clearBoard() {
        strokes.clear();
        invalidate();
    }

    public Bitmap getDrawingBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }
}
