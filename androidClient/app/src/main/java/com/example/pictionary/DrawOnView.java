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

/**
 * This class represents a custom View that allows the user to draw on it.
 */
public class DrawOnView extends View {
    /** constants */
    // starting color
    public final static int DEFAULT_COLOR = Color.BLACK;
    // size of brush
    public final static int STROKE_SIZE = 10;

    private final List<PathShape> strokes = new ArrayList<>();
    private static Paint paint;
    private final ClientController clientController = ClientController.getInstance();
    private @ColorInt int currColor = Color.BLACK;
    private PathShape currentPath;
    private boolean isDrawingEnabled = true;

    /**
     * Sets whether drawing is enabled on this view.
     * @param isDrawingEnabled Whether drawing is enabled.
     */
    public void setDrawingEnabled(boolean isDrawingEnabled) {
        this.isDrawingEnabled = isDrawingEnabled;
    }

    /**
     * Constructor for the DrawOnView class.
     * @param context The current context.
     */
    public DrawOnView(Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(DEFAULT_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_SIZE);
    }

    /**
     * Called when the view should render its content.
     * @param canvas The canvas on which to draw.
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        for (PathShape stroke : strokes) {
            stroke.draw(canvas, paint);
        }
    }

    /**
     * Called when a touch event is dispatched to this view.
     * @param event The motion event.
     * @return True if the event was handled, false otherwise.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isDrawingEnabled) {
            return false;
        }

        // get the x and y coordinates of the touch event
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // create a new path shape and add it to the list of strokes
                currentPath = new PathShape(x, y, currColor);
                strokes.add(currentPath);
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    // update the last path shape with the new point
                    currentPath.updatePoint(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
                // update the last path shape with the new point
                if (currentPath != null) {
                    currentPath.updatePoint(x, y);
                }
                // send the whole drawing to the server
                clientController.sendBitmap(getDrawingBitmap());
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    /**
     * Sets the color of the paint used to draw.
     * @param color The new color.
     */
    public void setColor(int color) {
        paint.setColor(color);
        currColor = color;
    }

    /**
     * Removes the last stroke from the list of strokes and updates the server.
     */
    public void undo() {
        if (!strokes.isEmpty()) {
            strokes.remove(strokes.size() - 1);
            clientController.sendBitmap(getDrawingBitmap());
            invalidate();
        }
    }

    /**
     * Clears all strokes from the list of strokes and updates the server.
     */
    public void clear() {
        strokes.clear();
        clientController.sendBitmap(getDrawingBitmap());
        invalidate();
    }

    /**
     * Returns a bitmap representation of the current drawing.
     * @return The bitmap representation of the current drawing.
     */
    public Bitmap getDrawingBitmap() {
        if (getWidth() == 0 || getHeight() == 0) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }
}