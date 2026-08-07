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
 * <i>a</i> and <i>b</i> are in units of one third of the smallest
 * Tangram piece, i.e. <i>a</i> = 3 is the length of the congruent
 * sides of a small triangle and <i>b</i> = 3 is the length of its
 * hypotenuse.  In mathematical terms, this is the
 * <a href="https://mathworld.wolfram.com/QuadraticField.html">Real
 * Quadratic Field</a> &#8474;[&#8730;2].
 *
 * @author Trevin Beattie
 */
public abstract class TPoint implements Parcelable {

    double SQRT2 = Math.sqrt(2);

    /** JSON key for the <i>a</i> coefficient of the X coordinate */
    String JSON_X_A = "xa";

    /** JSON key for the <i>b</i> coefficient of the X coordinate */
    String JSON_X_B = "xb";

    /** JSON key for the <i>a</i> coefficient of the Y coordinate */
    String JSON_Y_A = "ya";

    /** JSON key for the <i>b</i> coefficient of the Y coordinate */
    String JSON_Y_B = "yb";

    /** @return the <i>a</i> coefficient of the <i>x</i> coordinate */
    public abstract float getXa();

    /** @return the <i>b</i> coefficient of the <i>x</i> coordinate */
    public abstract float getXb();

    /** @return the <i>a</i> coefficient of the <i>y</i> coordinate */
    public abstract float getYa();

    /** @return the <i>b</i> coefficient of the <i>y</i> coordinate */
    public abstract float getYb();

    /**
     * @return the X coordinate of this point
     */
    public float getX() {
        return (float) (getXa() + getXb() * SQRT2);
    }

    /**
     * @return the Y coordinate of this point
     */
    public float getY() {
        return (float) (getYa() + getYb() * SQRT2);
    }

    /**
     * Translate this point by a given amount, i.e. add two points together.
     *
     * @param p2 the point to add to this one
     *
     * @return the translated point
     */
    public abstract @NonNull TPoint add(@NonNull TPoint p2);

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
    public abstract @NonNull TPoint coarseRotate(int steps);

    /**
     * Rotate this point by an arbitrary amount.  If {@code degrees} is
     * at least a multiple of 45&deg;, this uses {@link #coarseRotate(int)}.
     * For any remaining angle not a multiple of 45&deg;, this adjusts the
     * <i>a</i> and <i>b</i> components of each coordinate separately;
     * the results will not typically be integer values or simple
     * binary fractions.
     *
     * @param degrees the angle to rotate the point about the origin
     * in degrees.
     *
     * @return the rotated point
     */
    public abstract @NonNull TPoint rotate(double degrees);

    /**
     * Reverse the X coordinate of this point (horizontal mirror).
     *
     * @return the point at (-X, Y) of this point
     */
    public abstract @NonNull TPoint mirrorX();

    /**
     * Reverse the Y coordinate of this point (vertical mirror).
     *
     * @return the point at (X, -Y) of this point
     */
    public abstract @NonNull TPoint mirrorY();

    /**
     * Map this point to a JSON object for saving to a file.
     *
     * @return the JSON object
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        if (getXb() == 0 || getXa() != 0)
            json.put(JSON_X_A, getXa());
        if (getXb() != 0)
            json.put(JSON_X_B, getXb());
        if (getYb() == 0 || getYa() != 0)
            json.put(JSON_Y_A, getYa());
        if (getYb() != 0)
            json.put(JSON_Y_B, getYb());
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
        /*
         * Since this is an interface, we MUST write the type
         * of implementing class first so that when reading
         * it back we know which implementation to create.
         * We'll use a simple integer to indicate whether it's
         * an ImmutableTPoint (0) or MutableTPoint (non-0).
         */
        dest.writeInt((this instanceof ImmutableTPoint) ? 0 : 1);
        dest.writeFloat(getXa());
        dest.writeFloat(getXb());
        dest.writeFloat(getYa());
        dest.writeFloat(getYb());
    }

    /**
     * Create a point from a {@link Parcel}.
     */
    public static Creator<TPoint> CREATOR = new Creator<>() {
        @Override
        public TPoint createFromParcel(android.os.Parcel in) {
            boolean isMutable = (in.readInt() != 0);
            return isMutable ? MutableTPoint.CREATOR.createFromParcel(in)
                    : ImmutableTPoint.CREATOR.createFromParcel(in);
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
