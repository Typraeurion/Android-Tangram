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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Superclass of all pieces of a Tangram.  These are polygons whose
 * angles are in multiples of 45&deg;.  Vertices of the polygon are
 * given on a unit scale using a two-integer system of the formula
 * <i>a</i> + <i>b</i>&#8730;2&#773; for each of the <i>x</i> and
 * <i>y</i> coordinates where both <i>a</i> and <i>b</i> are
 * relative to the closest point to the centroid of the polygon.
 *
 * @author Trevin Beattie
 */
public abstract class TangramPiece implements Parcelable {

    public static final String LOG_TAG = "TangramPiece";

    /** JSON key for the name of the piece **/
    public static final String JSON_NAME = "name";

    /** JSON key for the rotation of the piece **/
    public static final String JSON_ROTATION = "rotation";

    /** JSON key for the position of the piece **/
    public static final String JSON_POSITION = "position";

    /** JSON key for whether this piece is mirrored */
    public static final String JSON_MIRRORED = "isMirrored";

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

    /** @return the name of the piece to use in JSON puzzle files */
    public abstract String getJsonName();

    /**
     * @return the current orientation of this piece as a
     * multiple of 45&deg; or <sup>&pi;</sup>&#8260;<sub>4</sub>
     * radians.
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * Set the orientation of this piece as a multiple of 45&deg;
     * or <sup>&pi;</sup>&#8260;<sub>4</sub> radians.
     *
     * @param orientation the new orientation [0&ndash;8)
     */
    public void setRotation(float orientation) {
        rotation = (float) (orientation - 8 * Math.floor(orientation / 8));
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
        setRotation(rotation + degreesClockwise / 45.0f);
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
    public abstract int getDrawableId();

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
            Log.e(LOG_TAG, String.format(Locale.US,
                    "No drawable with ID %d", getDrawableId()));
            // Use a non-zero fallback size
            return 10;
        }
        return Math.max(drawable.getMinimumWidth(),
                drawable.getMinimumHeight());
    }

    /**
     * Map this piece to a JSON object for saving to a file.
     *
     * @return the JSON object
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(JSON_NAME, getJsonName());
        json.put(JSON_POSITION, position.toJSON());
        if (canFlip())
            json.put(JSON_MIRRORED, isMirrored);
        json.put(JSON_ROTATION, rotation);
        return json;
    }

    /**
     * Create a piece from a JSON object.
     *
     * @throws JSONException if any required fields are missing or
     * if any of the values in the JSON object are invalid.
     */
    public static TangramPiece fromJSON(JSONObject json) throws JSONException {
        String name = json.getString(JSON_NAME);
        TangramPiece piece = switch(name) {
            case TangramSmallTriangle.JSON_NAME -> new TangramSmallTriangle();
            case TangramSquare.JSON_NAME -> new TangramSquare();
            case TangramParallelogram.JSON_NAME -> new TangramParallelogram();
            case TangramMediumTriangle.JSON_NAME -> new TangramMediumTriangle();
            case TangramLargeTriangle.JSON_NAME -> new TangramLargeTriangle();
            default -> null;
        };
        if (piece == null)
            throw new JSONException("Unknown piece name: " + name);
        piece.position = new TPoint(json.getJSONObject(JSON_POSITION));
        if (piece.canFlip())
            piece.isMirrored = json.getBoolean(JSON_MIRRORED);
        piece.rotation = (float) json.getDouble(JSON_ROTATION);
        return piece;
    }

    /**
     * Save this piece in a {@link Parcel} as part of a {@link TangramPuzzle}.
     *
     * @param dest The {@link Parcel} in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     * This class does not use any flags.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // Since this is an abstract class, we MUST write
        // the type of child class first so that when reading
        // it back we know which implementation to create.
        dest.writeString(getJsonName());
        // Write the point's fields directly (paired with
        // TPoint.CREATOR.createFromParcel in our CREATOR below).  Do NOT
        // use writeParcelable here: that would prepend the creator class
        // name, which createFromParcel does not read back.
        position.writeToParcel(dest, flags);
        dest.writeFloat(rotation);
        dest.writeByte((byte) (isMirrored ? 1 : 0));
    }

    /**
     * Create a piece from a {@link Parcel}.  We can do this here in the
     * abstract class because none of the concrete implementations add
     * any of their own fields.
     */
    public static final Creator<TangramPiece> CREATOR = new Creator<>() {
        @Override
        public TangramPiece createFromParcel(Parcel in) {
            String name = in.readString();
            TangramPiece piece = (name == null) ? null : switch(name) {
                case TangramSmallTriangle.JSON_NAME -> new TangramSmallTriangle();
                case TangramSquare.JSON_NAME -> new TangramSquare();
                case TangramParallelogram.JSON_NAME -> new TangramParallelogram();
                case TangramMediumTriangle.JSON_NAME -> new TangramMediumTriangle();
                case TangramLargeTriangle.JSON_NAME -> new TangramLargeTriangle();
                default -> null;
            };
            if (piece == null)
                throw new ParcelFormatException(name == null
                        ? "Missing piece name"
                        : "Unknown piece name: " + name);
            piece.position = TPoint.CREATOR.createFromParcel(in);
            piece.rotation = in.readFloat();
            piece.isMirrored = in.readByte() != 0;
            return piece;
        }
        @Override
        public TangramPiece[] newArray(int size) {
            return new TangramPiece[size];
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
