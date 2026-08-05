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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Container for a Tangram.  This names a puzzle and provides the
 * placement of its pieces.
 *
 * @author Trevin Beattie
 */
public class TangramPuzzle implements Parcelable {

    public static final String LOG_TAG = "TangramPuzzle";

    /** JSON key for the ID of the puzzle */
    public static final String JSON_ID = "id";

    /** JSON key for the name of the puzzle */
    public static final String JSON_NAME = "name";

    /** JSON key for the size of the puzzle */
    public static final String JSON_SIZE = "size";

    /** JSON Key for the array of pieces of the puzzle */
    public static final String JSON_PIECES = "pieces";

    /**
     * The file that the puzzle came from, if any.
     * Not set if the puzzle is created in-game.
     */
    protected @Nullable String sourceFileName;

    /**
     * Identifier for the puzzle from the JSON file; should be
     * unique among all puzzles in the same file.  Not set
     * for puzzles which haven&rsquo;t been read from a file
     * or stored to a file yet.
     */
    protected @Nullable String id;

    /** Display name of the puzzle.  This may be a translation. */
    protected @Nullable String name;
    /**
     * The larger of width or height of the puzzle.
     * The value must be at least 12, which is the size of the
     * square the Tangram pieces are cut from.
     */
    protected float size;
    protected @NonNull TangramPiece[] pieces;

    /**
     * Default constructor.  The puzzle is unnamed and will have
     * the seven standard pieces placed outside the bounds.
     */
    public TangramPuzzle() {
        // Set the default bounds of the puzzle
        // to the size of 9 (3×3) compact squares.
        size = 36;
        pieces = new TangramPiece[7];
        pieces[0] = new TangramSmallTriangle();
        pieces[1] = new TangramSmallTriangle();
        pieces[2] = new TangramSquare();
        pieces[3] = new TangramParallelogram();
        pieces[4] = new TangramMediumTriangle();
        pieces[5] = new TangramLargeTriangle();
        pieces[6] = new TangramLargeTriangle();
        for (int i = 0; i < pieces.length; i++)
            pieces[i].setPosition(new TPoint(12*i - 36, 0, 24, 0));
    }

    /**
     * Create a puzzle from a JSON object.
     *
     * @throws InvalidPuzzleException if the puzzle does not contain
     * the seven required pieces.
     * @throws JSONException if any required field is missing or
     * if any of the values in the JSON object are invalid.
     */
    public TangramPuzzle(JSONObject json)
            throws InvalidPuzzleException, JSONException {
        id = json.getString(JSON_ID);
        name = json.getString(JSON_NAME);
        // To Do: Check for a translation table
        size = (float) json.getDouble(JSON_SIZE);
        JSONArray jsonPieces = json.getJSONArray(JSON_PIECES);
        pieces = new TangramPiece[jsonPieces.length()];
        for (int i = 0; i < pieces.length; i++) try {
            pieces[i] = TangramPiece.fromJSON(jsonPieces.getJSONObject(i));
        } catch (JSONException e) {
            JSONException wrappedException = new JSONException(
                    String.format(Locale.US,
                    "Invalid piece in puzzle \"%s\" at index %d",
                    name, i));
            // We have to do this separate from the constructor since
            // older Android SDK's did not implement the 2-arg constructor.
            wrappedException.initCause(e);
            throw wrappedException;
        }
        if (!isValid())
            throw new InvalidPuzzleException("Invalid puzzle: " + name);
    }

    /**
     * Create a puzzle from a {@link Parcel}.
     */
    private TangramPuzzle(Parcel in) {
        sourceFileName = in.readString();
        id = in.readString();
        name = in.readString();
        size = in.readFloat();
        TangramPiece[] piecesIn = in.createTypedArray(TangramPiece.CREATOR);
        if (piecesIn != null) {
            pieces = piecesIn;
        } else {
            Log.e(LOG_TAG, "TangramPuzzle parcel has no pieces!");
            pieces = new TangramPiece[0];
        }
    }

    /**
     * @return the file that the puzzle came from, or
     * {@code null} if the puzzle was created in-game.
     */
    public @Nullable String getSourceFileName() {
        return sourceFileName;
    }

    /**
     * Set the name of the file that the puzzle came from.
     */
    public void setSourceFileName(@NonNull String fileName) {
        sourceFileName = fileName;
    }

    /**
     * @return the ID of the puzzle, or {@code null}
     * if the puzzle has no ID yet.
     */
    public @Nullable String getId() {
        return id;
    }

    /**
     * Set the ID of the puzzle
     *
     * @param newId the ID to set
     */
    public void setId(@NonNull String newId) {
        id = newId;
    }

    /**
     * @return the display name of the puzzle, or
     * an empty string if it has no name.
     */
    public @NonNull String getName() {
        return (name == null) ? "" : name;
    }

    /**
     * Set the display name of the puzzle
     */
    public void setName(@NonNull String newName) {
        name = newName;
    }

    /** @return the number of pieces in the puzzle (normally 7) */
    public int getPieceCount() {
        return pieces.length;
    }

    /**
     * Get one of the pieces in this puzzle.
     *
     * @param index the index of the piece to retrieve.
     *
     * @return the puzzle piece at the given index
     */
    public TangramPiece getPiece(int index) {
        return pieces[index];
    }

    /**
     * @return the size of the puzzle (in Tangram piece units).
     */
    public float getSize() {
        return size;
    }

    private static final Map<Class<? extends TangramPiece>, Integer>
            EXPECTED_PIECES = Map.of(TangramLargeTriangle.class, 2,
            TangramMediumTriangle.class, 1,
            TangramParallelogram.class, 1,
            TangramSquare.class, 1,
            TangramSmallTriangle.class, 2);

    /**
     * Check whether this is a valid Tangram.  To be valid,
     * it must contain exactly seven pieces:
     * <ul>
     *     <li>2 large triangles</li>
     *     <li>1 medium triangle</li>
     *     <li>1 parallelogram</li>
     *     <li>1 square</li>
     *     <li>2 small triangles</li>
     * </ul>
     * and all pieces must be touching and not overlap.
     * Ideally the pieces should be oriented at multiples of 45&deg;
     * (or in a few cases a multiple of 22.5&deg;), aligned to
     * puzzle grid units, and centered around the origin, but this
     * method doesn&rsquo;t impose a hard constraint on that.
     * <p>
     *     <i>Note: the method currently doesn&rsquo;t check for
     *     overlaps or non-touching pieces, while we&rsquo;re working
     *     on creating the puzzle library&hellip;</i>
     * </p>
     *
     * @return true if all expected pieces are present, false otherwise
     */
    public boolean isValid() {
        if (pieces.length != 7)
            return false;
        Map<Class<? extends TangramPiece>, Integer> counts = new HashMap<>();
        for (TangramPiece piece : pieces) {
            Class<? extends TangramPiece> pieceClass = piece.getClass();
            if (!counts.containsKey(pieceClass))
                counts.put(pieceClass, 0);
            counts.put(pieceClass, counts.get(pieceClass) + 1);
        }
        return counts.equals(EXPECTED_PIECES);
    }

    /**
     * Map this puzzle to a JSON object for saving to a file.
     *
     * @return the JSON object
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(JSON_ID, id);
        json.put(JSON_NAME, name);
        json.put(JSON_SIZE, size);
        JSONArray jsonPieces = new JSONArray();
        for (TangramPiece piece : pieces)
            jsonPieces.put(piece.toJSON());
        json.put(JSON_PIECES, jsonPieces);
        return json;
    }

    /**
     * Save this puzzle as a {@link Parcel} to pass it between activities.
     *
     * @param dest The {@link Parcel} in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     * This class does not use any flags.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(sourceFileName);
        dest.writeString(id);
        dest.writeString(name);
        dest.writeFloat(size);
        // Use writeTypedArray (paired with createTypedArray in our CREATOR):
        // the element type is known from TangramPiece.CREATOR, so this omits
        // the redundant per-element class name that writeParcelableArray adds.
        dest.writeTypedArray(pieces, flags);
    }

    /**
     * Create a puzzle from a {@link Parcel}.
     */
    public static final Creator<TangramPuzzle> CREATOR = new Creator<>() {
        @Override
        public TangramPuzzle createFromParcel(Parcel in) {
            return new TangramPuzzle(in);
        }
        @Override
        public TangramPuzzle[] newArray(int size) {
            return new TangramPuzzle[size];
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

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName())
                .append('[');
        if (sourceFileName != null)
            sb.append("source=\"").append(sourceFileName).append("\", ");
        if (id != null)
            sb.append("id=\"").append(id).append("\", ");
        if (name != null)
            sb.append("name=\"").append(name).append("\", ");
        sb.append("size=").append(size);
        sb.append(']');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (sourceFileName != null)
            hash += sourceFileName.hashCode();
        hash *= 31;
        if (id != null)
            hash += id.hashCode();
        hash *= 31;
        if (name != null)
            hash += name.hashCode();
        hash *= 31;
        hash += Float.hashCode(size);
        hash *= 31;
        for (TangramPiece piece : pieces)
            hash += piece.hashCode();
        return hash;
    }

    /**
     * For two TangramPuzzles to be the same, they must have the same
     * source, ID, and pieces.  We&rsquo;ll let the names be different
     * because one may be translated differently than the other, and
     * the sizes are only advisory.
     *
     * @param o the other object to compare this to
     *
     * @return {@code true} if the object is a TangramPuzzle identical to
     * this one, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof TangramPuzzle p2) {
            if (!Objects.equals(sourceFileName, p2.sourceFileName))
                return false;
            if (!Objects.equals(id, p2.id))
                return false;
            return Arrays.equals(pieces, p2.pieces);

        }
        return false;
    }

}
