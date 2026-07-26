package com.xmission.trevin.android.tangram.data;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

/**
 * Superclass of all pieces of a Tangram.  These are polygons whose
 * angles are in multiples of 45&deg;.  Vertices of the polygon are
 * given on a unit scale using a two-integer system of the formula
 * <i>a</i> + <i>b</i>&#8730;2&#773; for each of the <i>x</i> and
 * <i>y</i> coordinates where both <i>a</i> and <i>b</i> are
 * relative to the closest point to the centroid of the polygon.
 */
public abstract class TangramPiece {

    /**
     * Current orientation from the baseline around the centroid
     * in units of 45&deg; or <sup>&pi;</sup>&#8260;<sub>4</sub>
     * radians.  Normalized values are integers from 0&ndash;7,
     * but this may take fractional values for in-motion animation
     * or for certain puzzles where a piece only connects at a
     * tip (e.g. a candle&rsquo;s flame or cat&rsquo;s tail).
     */
    protected float rotation = 0;

    /**
     * Current position of the centroid of this piece within
     * the Tangram puzzle, in puzzle-based units.
     */
    protected @NonNull TPoint position = TPoint.ORIGIN;

    /**
     * Whether this piece is mirrored or flipped.  This can
     * only be changed if the piece has no reflective symmetry.
     */
    protected boolean isMirrored = false;

    /**
     * @return the current orientation of this piece as a
     * multiple of 45&deg; or <sup>&pi;</sup>&#8260;<sub>4</sub>
     * radians.
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * Rotate this piece by a multiple of 45&deg;.
     *
     * @param steps the number of 45&deg; steps by which to
     * rotate this piece.  Positive values rotate clockwise,
     * negative values rotate counter-clockwise.
     */
    public void coarseRotate(int steps) {
        rotation += steps % 8;
        if (rotation < 0)
            rotation += 8;
        else if (rotation >= 8)
            rotation -= 8;
    }

    /**
     * Rotate this piece by an arbitrary amount.
     *
     * @param degreesClockwise the amount by which to rotate the piece
     * in degrees.  May be negative to rotate counter-clockwise.
     */
    public void fineRotateDegrees(float degreesClockwise) {
        rotation += degreesClockwise / 45.0f;
        rotation = (float) (rotation - 8 * Math.floor(rotation / 8));
    }

    /**
     * @return the current position of this piece
     */
    public @NonNull TPoint getPosition() {
        return position;
    }

    /**
     * Move this piece to a given position in the puzzle.
     */
    public void setPosition(@NonNull TPoint position) {
        this.position = position;
    }

    /**
     * @return whether this piece can be flipped over.
     */
    public abstract boolean canFlip();


    /**
     * @return whether this piece has been flipped over
     */
    public boolean isMirrored() {
        return isMirrored;
    }

    /**
     * Flip over the piece.  Does nothing if the piece
     * has reflective symmetry.
     */
    public void flip() {
        if (!canFlip())
            return;
        isMirrored = !isMirrored;
    }

    /**
     * @return the unmodified set of vertices of this piece.
     * All subclasses must implement this.
     */
    protected abstract TPoint[] getShapeVertices();

    /**
     * @return the vertices of this piece in its current
     * orientation and position.
     */
    public TPoint[] getVertices() {
        TPoint[] transformed = new TPoint[getShapeVertices().length];
        for (int i = 0; i < transformed.length; i++) {
            TPoint v = getShapeVertices()[i];
            if (isMirrored && v.getX() != 0)
                v = v.mirrorX();
            if (rotation != 0) {
                if (rotation >= 1)
                    v = v.coarseRotate((int) rotation);
                double rad = (rotation - (int) rotation) * Math.PI / 4;
                if (rad != 0)
                    v = v.fineRotate(rad);
            }
            transformed[i] = v.add(position);
        }
        return transformed;
    }

    /**
     * @return the radius from the piece&rsquo;s centroid
     * to the farthest vertex.
     */
    public abstract float getMaxRadius();

    /**
     * @return the ID of the Android drawable used to draw this piece.
     */
    protected abstract int getDrawableId();

    /**
     * @return the ID of the theme attribute (e.g.
     * {@code R.attr.tangramSmallTriangleColor}) giving the fill color
     * for this piece.  The current theme determines the actual color,
     * which lets the play mode restyle the pieces.
     */
    public abstract int getColorAttr();

    /**
     * @param context the context in which to load the drawable
     *
     * @return the size (width &amp; height) of the image needed to
     * render the drawable for this piece at any orientation.
     */
    public int getDrawableSize(Context context) {
        Drawable drawable = AppCompatResources.getDrawable(
                context, getDrawableId());
        if (drawable == null) {
            Log.e(getClass().getSimpleName(),
                    "No drawable with ID " + getDrawableId());
            // Use a non-zero fallback size
            return 10;
        }
        return Math.max(drawable.getMinimumWidth(),
                drawable.getMinimumHeight());
    }

}
