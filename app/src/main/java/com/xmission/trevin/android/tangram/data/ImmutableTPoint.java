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

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An unmodifiable implementation of {@link TPoint}.  Operations
 * performed on this will return a {@link MutableTPoint}.
 */
public class ImmutableTPoint extends TPoint {

    /** The origin of the coordinate space. */
    public static ImmutableTPoint ORIGIN =
            new ImmutableTPoint(0f, 0f, 0f, 0f);

    private final float xa, xb, ya, yb;

    /**
     * Construct a point with coordinates (<i>a<sub>x</sub></i>
     * + <i>b<sub>x</sub></i>&#8730;2&#773;) <i>X</i>, (<i>a<sub>y</sub></i>
     * + <i>b<sub>y</sub></i>&#8730;2&#773;) <i>Y</i>.
     */
    public ImmutableTPoint(float ax, float bx, float ay, float by) {
        xa = ax;
        xb = bx;
        ya = ay;
        yb = by;
    }

    /**
     * Construct a point from a {@link Parcel}.
     */
    private ImmutableTPoint(android.os.Parcel in) {
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
    public ImmutableTPoint(JSONObject json) throws JSONException {
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

    @Override
    public float getXb() {
        return xb;
    }

    @Override
    public float getYa() {
        return ya;
    }

    @Override
    public float getYb() {
        return yb;
    }

    /**
     * @return a {@link TPoint} with the result, which may be either
     * this point or {@code p2} if the other {@link TPoint} is the
     * {@link #ORIGIN}.
     */
    @Override
    public @NonNull TPoint add(@NonNull TPoint p2) {
        if (p2.equals(ORIGIN))
            return this;
        return new MutableTPoint(xa + p2.getXa(), xb + p2.getXb(),
                ya + p2.getYa(), yb + p2.getYb());
    }

    /**
     * @return the rotated point, which may be this
     * if {@code steps} &#8801; 0 (mod 8)
     */
    @Override
    public @NonNull TPoint coarseRotate(int steps) {
        steps = Math.floorMod(steps, 8);
        return switch (steps) {
            case 1 -> new MutableTPoint(xb - yb, xa / 2 - ya / 2,
                    xb + yb, xa / 2 + ya / 2);
            case 2 -> new MutableTPoint(-ya, -yb, xa, xb);
            case 3 -> new MutableTPoint(-xb - yb, -xa / 2 - ya / 2,
                    xb - yb, xa / 2 - ya / 2);
            case 4 -> new MutableTPoint(-xa, -xb, -ya, -yb);
            case 5 -> new MutableTPoint(yb - xb, ya / 2 - xa / 2,
                    -xb - yb, -xa / 2 - ya / 2);
            case 6 -> new MutableTPoint(ya, yb, -xa, -xb);
            case 7 -> new MutableTPoint(xb + yb, xa / 2 + ya / 2,
                    -xb + yb, -xa / 2 + ya / 2);
            default -> this; // No change
        };
    }

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
     * @return the rotated point, which may be this
     * if {@code degrees} &#8801; 0 (mod 360)
     */
    public @NonNull TPoint rotate(double degrees) {
        if (degrees == 0)
            return this;
        // For rotations in an integer multiple
        // of 45 degrees, use coarse rotation.
        int steps = (int) (degrees / 45);
        if ((steps != 0) && (degrees - steps * 45 == 0))
            return coarseRotate(steps);
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new MutableTPoint((float) (xa * cos - ya * sin),
                (float)(xb * cos - yb * sin),
                (float) (xa * sin + ya * cos),
                (float) (xb * sin + yb * cos));
    }

    /**
     * @return the point at (-X, Y) of this point, which may be this
     * if X is 0.
     */
    @Override
    public @NonNull TPoint mirrorX() {
        if ((xa == 0) && (xb == 0))
            return this;
        return new MutableTPoint(-xa, -xb, ya, yb);
    }

    /**
     * @return the point at (X, -Y) of this point, which may be this
     * if Y is 0.
     */
    @Override
    public @NonNull TPoint mirrorY() {
        if ((ya == 0) && (yb == 0))
            return this;
        return new MutableTPoint(xa, xb, -ya, -yb);
    }

    @Override
    public @NonNull String toString() {
        StringBuilder sb = new StringBuilder("ImmutableTPoint(");
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
     * Immutable points don&rsquo;t require cloning.
     *
     * @return this
     */
    @Override
    public @NotNull ImmutableTPoint clone() {
        return this;
    }

    /**
     * Create a point from a {@link Parcel}.
     */
    public static final Creator<ImmutableTPoint> CREATOR = new Creator<>() {
        @Override
        public ImmutableTPoint createFromParcel(Parcel in) {
            return new ImmutableTPoint(in);
        }
        @Override
        public ImmutableTPoint[] newArray(int size) {
            return new ImmutableTPoint[size];
        }
    };

}
