package com.example.pictionary;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.annotation.ColorInt;


/**
 * This class represents a drawable path with a specific color.
 * It provides methods to draw the path on a canvas, set the color of the path, and update the path with a new point.
 */
public class PathShape {
    // the path
    private final Path path = new Path();
    // the color of the path
    private @ColorInt int color;

    /**
     * Constructor for the PathShape class.
     * @param startX The x-coordinate of the start point of the path.
     * @param startY The y-coordinate of the start point of the path.
     * @param color The color of the path.
     */
    public PathShape(float startX, float startY, int color) {
        this.color = color;
        path.moveTo(startX, startY);
    }

    /**
     * Draws the path on a canvas with a specific paint.
     * @param canvas The canvas on which the path is to be drawn.
     * @param paint The paint with which the path is to be drawn.
     */
    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(color);
        canvas.drawPath(path, paint);
    }

    /**
     * Sets the color of the path.
     * @param color The new color of the path.
     */
    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Updates the path with a new point.
     * @param xPathUpdate The x-coordinate of the new point.
     * @param yPathUpdate The y-coordinate of the new point.
     */
    public void updatePoint(float xPathUpdate, float yPathUpdate) {
        path.lineTo(xPathUpdate, yPathUpdate);
    }
}