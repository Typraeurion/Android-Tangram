/*
 * Copyright © 2026 Trevin Beattie
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.xmission.trevin.android.tangram.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A point in Tangram space.  Numbers in the coordinate system are
 * given in the form <i>a</i> + <i>b</i>&#8730;2&#773; where both
 * <i>a</i> and <i>b</i> are in units of the smallest Tangram
 * piece, i.e. <i>a</i> is one side length of a small triangle and
 * <i>b</i> is one diagonal length of the same.  This object is
 * immutable; operations on it will return a new point.
 *
 * @author Trevin Beattie
 */
public class TPoint implements Parcelable {

    public static final double SQRT2 = Math.sqrt(2);

    /** JSON key for the <i>a</i> coefficient of the X coordinate */
    public static final String JSON_X_A = "xa";

    /** JSON key for the <i>b</i> coefficient of the X coordinate */
    public static final String JSON_X_B = "xb";

    /** JSON key for the <i>a</i> coefficient of the Y coordinate */
    public static final String JSON_Y_A = "ya";

    /** JSON key for the <i>b</i> coefficient of the Y coordinate */
    public static final String JSON_Y_B = "yb";

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

    /**
     * Construct a point from a {@link Parcel}.
     */
    private TPoint(Parcel in) {
        xa = in.readFloat();
        xb = in.readFloat();
        ya = in.readFloat();
        yb = in.readFloat();
    }

    /**
     * Construct a point from a JSON object.
     *
     * @throws JSONException if any of the values in the JSON object
     * are not valid floating-point numbers.
     */
    public TPoint(JSONObject json) throws JSONException {
        if (json.has(JSON_X_A))
            xa = (float) json.getDouble(JSON_X_A);
        else
            xa = 0;
        if (json.has(JSON_X_B))
            xb = (float) json.getDouble(JSON_X_B);
        else
            xb = 0;
        if (json.has(JSON_Y_A))
            ya = (float) json.getDouble(JSON_Y_A);
        else
            ya = 0;
        if (json.has(JSON_Y_B))
            yb = (float) json.getDouble(JSON_Y_B);
        else
            yb = 0;
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
            case 5 -> new TPoint(yb - xb, ya / 2 - xa / 2,
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

    /**
     * Map this point to a JSON object for saving to a file.
     *
     * @return the JSON object
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        if (xb == 0 || xa != 0)
            json.put(JSON_X_A, xa);
        if (xb != 0)
            json.put(JSON_X_B, xb);
        if (yb == 0 || ya != 0)
            json.put(JSON_Y_A, ya);
        if (yb != 0)
            json.put(JSON_Y_B, yb);
        return json;
    }

    /**
     * Save this point to a {@link Parcel} (typically as part of a
     * {@link TangramPiece}.
     *
     * @param dest The {@link Parcel} in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     * This class does not use any flags.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeFloat(xa);
        dest.writeFloat(xb);
        dest.writeFloat(ya);
        dest.writeFloat(yb);
    }

    /**
     * Create a point from a {@link Parcel}.
     */
    public static final Creator<TPoint> CREATOR =
            new Creator<TPoint>() {
                @Override
                public TPoint createFromParcel(Parcel in) {
                    return new TPoint(in);
                }
                @Override
                public TPoint[] newArray(int size) {
                    return new TPoint[size];
                }
            };

    /**
     * The {@link Parcel} for this object contains no special objects.
     *
     * @return 0
     */
    @Override
    public int describeContents() {
        return 0;
    }

}
