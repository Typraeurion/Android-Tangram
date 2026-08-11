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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A polygon in Tangram space.  This is created from a {@link TangramPuzzle}
 * by merging the collinear connected or overlapping edges of its pieces,
 * and used to compare puzzles for having the same silhouette.
 *
 * @author Trevin Beattie
 */
public class TPolygon implements Parcelable {

    /**
     * Maximum distance, in puzzle units, for two vertices to be
     * considered at the same point.
     */
    public static final double VERTEX_TOLERANCE = 0.1;

    /**
     * The edges of the polygon.  As a general rule, the list must be
     * kept in clockwise order (using the display coordinate system,
     * where +Y is down) and the start point of each edge must match
     * the end point of the previous edge, wrapping back around to
     * close the polygon.
     */
    private final List<TEdge> edges = new ArrayList<>();

    /**
     * Construct a polygon from a {@link Parcel}.
     */
    private TPolygon(Parcel in) {
        in.readTypedList(edges, TEdge.CREATOR);
    }

    /**
     * Merge the edges of a TangramPiece with the current polygon.
     * If this is the first piece added, its edges are simply copied.
     * Otherwise it <i>must</i> touch at least one of the edges or
     * vertices of the polygon.
     *
     * @param piece the piece to merge
     */
    public void merge(@NonNull TangramPiece piece) {
        if (edges.isEmpty()) {
            Collections.addAll(edges, piece.getEdges());
            // If the piece was mirrored, its edges will be reversed
            if (piece.isMirrored())
                Collections.reverse(edges);
            return;
        }
        // To Do: Finish implementing method stub
    }

    /**
     * Compare this polygon with another to see whether they are
     * identical, i.e. they have the same shape.  This is a
     * <i>destructive</i> operation in that it can change the
     * order and position of the edges, so {@link #merge(TangramPiece)}
     * <i>must not</i> be called after this call.
     */
    public boolean isIdenticalTo(TPolygon poly2) {
        // Shortcut out: If the polygons have different
        // numbers of edges, they cannot be identical.
        if (edges.size() != poly2.edges.size())
            return false;
        // To Do: Center both polygons
        for (int i = 0; i < edges.size(); i++) {
            TPoint v1 = edges.get(i).getStart();
            TPoint v2 = poly2.edges.get(i).getStart();
            if (v1.distanceTo(v2) > VERTEX_TOLERANCE)
                return false;
        }
        return true;
    }

    /**
     * Save this polygon to a {@link Parcel}.
     *
     * @param dest The {@link Parcel} in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     * This class does not use any flags.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedList(edges);
    }

    /**
     * Create a polygon from a {@link Parcel}.
     */
    public static Creator<TPolygon> CREATOR = new Creator<>() {
        @Override
        public TPolygon createFromParcel(Parcel in) {
            return new TPolygon(in);
        }
        @Override
        public TPolygon[] newArray(int size) {
            return new TPolygon[size];
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

    /**
     * Return a string representation of this polygon.  Because all
     * edges are straight and should be connected in order, all we
     * should need are the starting points of each edge, plus the
     * end of the last edge to close the loop.
     */
    @Override
    public @NonNull String toString() {
        StringBuilder sb = new StringBuilder("TPolygon[");
        if (edges.isEmpty())
            return sb.append("empty]").toString();
        for (TEdge e : edges)
            sb.append(e.getStart()).append(" \u2192 ");
        sb.append(edges.get(edges.size() - 1).getEnd());
        sb.append(']');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (TEdge e : edges)
            hash = hash * 31 + e.hashCode();
        return hash;
    }

    /**
     * Compare this polygon with another object for <u>strict</u>
     * equality.  This means if the other object is a TPolygon,
     * its edges must be in the same order and same locations.
     * To check whether two polygons are <i>identical</i>
     * irrespective of order or location, use
     * {@link #isIdenticalTo(TPolygon)}.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TPolygon poly2))
            return false;
        return edges.equals(poly2.edges);
    }

}
