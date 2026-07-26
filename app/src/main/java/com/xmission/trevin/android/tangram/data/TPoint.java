package com.xmission.trevin.android.tangram.data;

import androidx.annotation.NonNull;

/**
 * A point in Tangram space.  Numbers in the coordinate system are
 * given in the form <i>a</i> + <i>b</i>&#8730;2&#773; where both
 * <i>a</i> and <i>b</i> are in units of the smallest Tangram
 * piece, i.e. <i>a</i> is one side length of a small triangle and
 * <i>b</i> is one diagonal length of the same.  This object is
 * immutable; operations on it will return a new point.
 */
public class TPoint {

    public static final double SQRT2 = Math.sqrt(2);

    protected final float xa, xb, ya, yb;

    /**
     * Construct a point with coordinates (<i>a<sub>x</sub></i>
     * + <i>b<sub>x</sub></i>&#8730;2&#773;) <i>X</i>, (<i>a<sub>y</sub></i>
     * + <i>b<sub>y</sub></i>&#8730;2&#773;) <i>Y</i>.
     */
    public TPoint(float ax, float bx, float ay, float by) {
        xa = ax;
        xb = bx;
        ya = ay;
        yb = by;
    }

    /** The origin of the puzzle space. */
    public static final TPoint ORIGIN = new TPoint(0f, 0f, 0f, 0f);

    @Override
    public @NonNull String toString() {
        StringBuilder sb = new StringBuilder("TPoint(");
        if (xb == 0)
            sb.append(xa);
        else if (xa == 0)
            sb.append(xb).append("√2\u0305");
        else
            sb.append(xa).append('+').append(xb).append("√2\u0305");
        sb.append("x, ");
        if (yb == 0)
            sb.append(ya);
        else if (ya == 0)
            sb.append(yb).append("√2\u0305");
        else
            sb.append(ya).append('+').append(yb).append("√2\u0305");
        sb.append("y)");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return ((Float.hashCode(xa) * 31 + Float.hashCode(xb))
                * 31 + Float.hashCode(ya)) * 31 + Float.hashCode(yb);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TPoint p2) {
            return p2.xa == xa && p2.xb == xb && p2.ya == ya && p2.yb == yb;
        }
        return false;
    }

    /**
     * @return the X coordinate of this point
     */
    public float getX() {
        return (float) (xa + xb * SQRT2);
    }

    /**
     * @return the Y coordinate of this point
     */
    public float getY() {
        return (float) (ya + yb * SQRT2);
    }

    /**
     * Translate this point by a given amount, i.e. add two points together.
     *
     * @param p2 the point to add to this one
     */
    public @NonNull TPoint add(TPoint p2) {
        return new TPoint(xa + p2.xa, xb + p2.xb, ya + p2.ya, yb + p2.yb);
    }

    /**
     * Rotate this point by a multiple of 45&deg;.  This works by swapping
     * and negating coefficients as needed so there is no loss of precision.
     *
     * @param steps the number of 45&deg; steps by which to rotate this point.
     * Positive values rotate clockwise, negative values rotate
     * counter-clockwise.
     *
     * @return the rotated point
     */
    public @NonNull TPoint coarseRotate(int steps) {
        steps = Math.floorMod(steps, 8);
        return switch (steps) {
            case 1 -> new TPoint(xb - yb, xa / 2 - ya / 2,
                    xb + yb, xa / 2 + ya / 2);
            case 2 -> new TPoint(-ya, -yb, xa, xb);
            case 3 -> new TPoint(-xb - yb, -xa / 2 - ya / 2,
                    xb - yb, xa / 2 - ya / 2);
            case 4 -> new TPoint(-xa, -xb, -ya, -yb);
            case 5 -> new TPoint(-xb - yb, -xa / 2 - ya / 2,
                    -xb - yb, -xa / 2 - ya / 2);
            case 6 -> new TPoint(ya, yb, -xa, -xb);
            case 7 -> new TPoint(xb + yb, xa / 2 + ya / 2,
                    -xb + yb, -xa / 2 + ya / 2);
            default -> this; // No change
        };
    }

    /**
     * Rotate this point by an arbitrary amount.  This adjusts the
     * <i>a</i> and <i>b</i> components of each coordinate separately;
     * the results will not typically be integer values or simple
     * binary fractions.
     *
     * @param radians the angle to rotate the point about the origin
     * in radians.
     *
     * @return the rotated point
     */
    public @NonNull TPoint fineRotate(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new TPoint((float) (xa * cos - ya * sin),
                (float)(xb * cos - yb * sin),
                (float) (xa * sin + ya * cos),
                (float) (xb * sin + yb * cos));
    }

    /**
     * Reverse the X coordinate of this point (horizontal mirror).
     *
     * @return the point at (-X, Y) of this point
     */
    public @NonNull TPoint mirrorX() {
        return new TPoint(-xa, -xb, ya, yb);
    }

    /**
     * Reverse the Y coordinate of this point (vertical mirror).
     *
     * @return the point at (X, -Y) of this point
     */
    public @NonNull TPoint mirrorY() {
        return new TPoint(xa, xb, -ya, -yb);
    }

}
