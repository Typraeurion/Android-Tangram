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

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An implementation of {@link TPoint} that may be transformed in-place.
 * Operations performed on this will modify its position.
 */
public class MutableTPoint extends TPoint {

    private float xa, xb, ya, yb;

    /**
     * Construct a point with coordinates (<i>a<sub>x</sub></i>
     * + <i>b<sub>x</sub></i>&#8730;2&#773;) <i>X</i>, (<i>a<sub>y</sub></i>
     * + <i>b<sub>y</sub></i>&#8730;2&#773;) <i>Y</i>.
     */
    public MutableTPoint(float ax, float bx, float ay, float by) {
        xa = ax;
        xb = bx;
        ya = ay;
        yb = by;
    }

    /**
     * Construct a point from a {@link Parcel}.
     */
    private MutableTPoint(android.os.Parcel in) {
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
    public MutableTPoint(JSONObject json) throws JSONException {
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

    @Override
    public float getXa() {
        return xa;
    }

    /**
     * Change the <i>a</i> coefficient of the X coordinate.
     *
     * @param a the new <i>a</i> coefficient
     */
    public void setXa(float a) {
        xa = a;
    }

    @Override
    public float getXb() {
        return xb;
    }

    /**
     * Change the <i>b</i> coefficient of the X coordinate.
     *
     * @param b the new <i>b</i> coefficient
     */
    public void setXb(float b) {
        xb = b;
    }

    @Override
    public float getYa() {
        return ya;
    }

    /**
     * Change the <i>a</i> coefficient of the Y coordinate.
     *
     * @param a the new <i>a</i> coefficient
     */
    public void setYa(float a) {
        ya = a;
    }

    @Override
    public float getYb() {
        return yb;
    }

    /**
     * Change the <i>b</i> coefficient of the Y coordinate.
     *
     * @param b the new <i>b</i> coefficient
     */
    public void setYb(float b) {
        yb = b;
    }

    /** @return this after translation */
    @Override
    public @NonNull MutableTPoint add(@NonNull TPoint p2) {
        xa += p2.getXa();
        xb += p2.getXb();
        ya += p2.getYa();
        yb += p2.getYb();
        return this;
    }

    /** @return this after rotation */
    @Override
    public @NonNull MutableTPoint coarseRotate(int steps) {
        steps = Math.floorMod(steps, 8);
        float xa2, xb2, ya2, yb2;
        switch (steps) {
            case 1:
                xa2 = xb - yb;
                xb2 = xa / 2 - ya / 2;
                ya2 = xb + yb;
                yb2 = xa / 2 + ya / 2;
                break;
            case 2:
                xa2 = -ya;
                xb2 = -yb;
                ya2 = xa;
                yb2 = xb;
                break;
            case 3:
                xa2 = -xb - yb;
                xb2 = -xa / 2 - ya / 2;
                ya2 = xb - yb;
                yb2 = xa / 2 - ya / 2;
                break;
            case 4:
                xa2 = -xa;
                xb2 = -xb;
                ya2 = -ya;
                yb2 = -yb;
                break;
            case 5:
                xa2 = yb - xb;
                xb2 = ya / 2 - xa / 2;
                ya2 = -xb - yb;
                yb2 = -xa / 2 - ya / 2;
                break;
            case 6:
                xa2 = ya;
                xb2 = yb;
                ya2 = -xa;
                yb2 = -xb;
                break;
            case 7:
                xa2 = xb + yb;
                xb2 = xa / 2 + ya / 2;
                ya2 = -xb + yb;
                yb2 = -xa / 2 + ya / 2;
                break;
            default:
                xa2 = xa;
                xb2 = xb;
                ya2 = ya;
                yb2 = yb;
                break;
        }
        xa = xa2;
        xb = xb2;
        ya = ya2;
        yb = yb2;
        return this;
    }

    /** @return this after rotation */
    @Override
    public @NonNull MutableTPoint rotate(double degrees) {
        if (degrees == 0)
            return this;
        // For rotations in an integer multiple
        // of 45 degrees, use coarse rotation.
        int steps = (int) (degrees / 45);
        if (steps != 0) {
            coarseRotate(steps);
            degrees -= steps * 45;
            if (degrees == 0)
                return this;
        }
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        float xa2 = (float) (xa * cos - ya * sin);
        float xb2 = (float) (xb * cos - yb * sin);
        float ya2 = (float) (xa * sin + ya * cos);
        float yb2 = (float) (xb * sin + yb * cos);
        xa = xa2;
        xb = xb2;
        ya = ya2;
        yb = yb2;
        return this;
    }

    /** @return this after negating X */
    @Override
    public @NonNull MutableTPoint mirrorX() {
        xa = -xa;
        xb = -xb;
        return this;
    }

    /** @return this after negating Y */
    @Override
    public @NonNull MutableTPoint mirrorY() {
        ya = -ya;
        yb = -yb;
        return this;
    }

    @Override
    public @NonNull String toString() {
        StringBuilder sb = new StringBuilder("TPoint(");
        if (xb == 0)
            sb.append(xa);
        else if (xa == 0)
            sb.append(xb).append("√2\u0305");
        else {
            sb.append(xa);
            if (xb >= 0)
                sb.append('+');
            sb.append(xb).append("√2\u0305");
        }
        sb.append("x, ");
        if (yb == 0)
            sb.append(ya);
        else if (ya == 0)
            sb.append(yb).append("√2\u0305");
        else {
            sb.append(ya);
            if (yb >= 0)
                sb.append('+');
            sb.append(yb).append("√2\u0305");
        }
        sb.append("y)");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return ((Float.hashCode(xa) * 31 + Float.hashCode(xb))
                * 31 + Float.hashCode(ya)) * 31 + Float.hashCode(yb);
    }

    /**
     * Compare this TPoint with another for equality.  TPoints are equal
     * only if both coefficients of each coordinate are equal; this does
     * not consider the final computed value of each coordinate.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof TPoint p2) {
            return (p2.getXa() == xa) && (p2.getXb() == xb)
                    && (p2.getYa() == ya) && (p2.getYb() == yb);
        }
        return false;
    }

    /**
     * Copy this point; use to make another point which can be modified
     * without disturbing the original.
     *
     * @return a point with the same coordinates as this one.
     */
    @Override
    public @NonNull MutableTPoint clone() {
        return new MutableTPoint(xa, xb, ya, yb);
    }

    /**
     * Create a point from a {@link Parcel}.
     */
    public static final Creator<MutableTPoint> CREATOR = new Creator<>() {
        @Override
        public MutableTPoint createFromParcel(Parcel in) {
            return new MutableTPoint(in);
        }
        @Override
        public MutableTPoint[] newArray(int size) {
            return new MutableTPoint[size];
        }
    };

}
