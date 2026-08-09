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

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.exception.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
     * Maximum angle between two edges, in degrees, for them to be treated
     * as parallel when snapping.
     */
    public static final double EDGE_ANGLE_TOLERANCE = 7.5;

    /**
     * Maximum perpendicular gap, in puzzle units, between two nearly-parallel
     * edges for them to count as touching.  Two collinear edges must also
     * overlap by more than this along their shared direction to be an edge
     * contact; a smaller overlap is an end-to-end (vertex) contact.
     */
    public static final double EDGE_PROXIMITY = 1.0;

    /**
     * Maximum distance, in puzzle units, for an endpoint or midpoint of the
     * snapped edge to be pulled onto an endpoint or midpoint of the neighbor
     * edge (snap rule 2.4).
     */
    public static final double POINT_PROXIMITY = 1.0;

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
    protected final List<TangramPiece> pieces = new ArrayList<>();

    /**
     * After validating a puzzle, if there are any problems (e.g.
     * missing / extra pieces, overlapping pieces, or pieces not
     * touching), this will be set to a list of the validation
     * errors.
     */
    protected @Nullable List<TangramException> validationErrors;

    /**
     * Default constructor.  The puzzle is unnamed and will have
     * no pieces yet; the pieces will need to be added later.
     */
    // To Do: If it turns out there's no need for this constructor, remove it.
    public TangramPuzzle() {
        // Set the default bounds of the puzzle
        // to the size of 9 (3×3) compact squares.
        size = 72;
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
        for (int i = 0; i < jsonPieces.length(); i++) try {
            pieces.add(TangramPiece.fromJSON(jsonPieces.getJSONObject(i)));
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
            throw new InvalidPuzzleException("Invalid puzzle: " + name,
                    R.string.ErrorInvalidNamedPuzzle, name, validationErrors);
    }

    /**
     * Create a puzzle from a {@link Parcel}.
     */
    private TangramPuzzle(Parcel in) {
        sourceFileName = in.readString();
        id = in.readString();
        name = in.readString();
        size = in.readFloat();
        List<TangramPiece> piecesIn =
                in.createTypedArrayList(TangramPiece.CREATOR);
        if (piecesIn != null) {
            pieces.addAll(piecesIn);
        } else {
            Log.e(LOG_TAG, "TangramPuzzle parcel has no pieces list!");
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
        return pieces.size();
    }

    /**
     * Get one of the pieces in this puzzle.
     *
     * @param index the index of the piece to retrieve.
     *
     * @return the puzzle piece at the given index
     */
    public TangramPiece getPiece(int index) {
        return pieces.get(index);
    }

    /**
     * @return the size of the puzzle (in Tangram piece units).
     */
    public float getSize() {
        return size;
    }

    /**
     * Add a piece to this puzzle if not already present.
     *
     * @param piece the piece to add
     *
     * @return {@code true} if the piece was added, {@code false}
     * if it was already in the puzzle.
     */
    public boolean addPiece(@NonNull TangramPiece piece) {
        if (pieces.contains(piece))
            return false;
        return pieces.add(piece);
    }

    /**
     * Add multiple pieces to this puzzle at once.
     *
     * @param pieces the pieces to add
     */
    public void addPieces(@NonNull List<TangramPiece> pieces) {
        this.pieces.addAll(pieces);
    }

    /**
     * Remove a piece from this puzzle.
     *
     * @param piece the piece to remove
     *
     * @return {@code true} if the piece was present and removed,
     * {@code false} if it was not in this puzzle.
     */
    public boolean removePiece(@NonNull TangramPiece piece) {
        return pieces.remove(piece);
    }

    /**
     * @return an unmodifiable, in-order view of this puzzle&rsquo;s pieces
     * (back-to-front, matching the order they were added)
     */
    @NonNull
    public List<TangramPiece> getPieces() {
        return Collections.unmodifiableList(pieces);
    }

    /**
     * Remove all pieces from this puzzle.
     */
    public void clear() {
        pieces.clear();
    }

    private static class PieceTypeInfo {
        public final String pieceName;
        public final int pieceNameId;
        public final int expectedCount;
        PieceTypeInfo(String pieceName, int pieceNameId, int expectedCount) {
            this.pieceName = pieceName;
            this.pieceNameId = pieceNameId;
            this.expectedCount = expectedCount;
        }
    }

    private static final Map<Class<? extends TangramPiece>, PieceTypeInfo>
            PIECE_INFO = Map.of(
            TangramLargeTriangle.class, new PieceTypeInfo(
                    "Large Triangle", R.string.PieceNameLargeTriangle, 2),
            TangramMediumTriangle.class, new PieceTypeInfo(
                    "Medium Triangle", R.string.PieceNameMediumTriangle, 1),
            TangramParallelogram.class, new PieceTypeInfo(
                    "Parallelogram", R.string.PieceNameParallelogram, 1),
            TangramSquare.class, new PieceTypeInfo(
                    "Square", R.string.PieceNameSquare, 1),
            TangramSmallTriangle.class, new PieceTypeInfo(
                    "Small Triangle", R.string.PieceNameSmallTriangle, 2));

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
        List<TangramException> errors = new ArrayList<>();
        Map<Class<? extends TangramPiece>, Integer> counts = new HashMap<>();
        for (TangramPiece piece : pieces) {
            Class<? extends TangramPiece> pieceClass = piece.getClass();
            if (!counts.containsKey(pieceClass))
                counts.put(pieceClass, 0);
            counts.put(pieceClass, counts.get(pieceClass) + 1);
        }
        for (Map.Entry<Class<? extends TangramPiece>, PieceTypeInfo> entry :
                PIECE_INFO.entrySet()) {
            if (!counts.containsKey(entry.getKey())) {
                errors.add(new MissingPieceException(
                        entry.getValue().expectedCount,
                        entry.getValue().pieceNameId,
                        entry.getValue().pieceName));
            }
            else if (counts.get(entry.getKey()) != entry.getValue().expectedCount) {
                int delta = counts.get(entry.getKey()) -
                        entry.getValue().expectedCount;
                if (delta < 0)
                    errors.add(new MissingPieceException(-delta,
                            entry.getValue().pieceNameId,
                            entry.getValue().pieceName));
                else
                    errors.add(new ExtraPieceException(delta,
                            entry.getValue().pieceNameId,
                            entry.getValue().pieceName));
            }
        }
        // To Do: Check for overlaps and gaps
        if (errors.isEmpty()) {
            validationErrors = null;
            return true;
        }
        validationErrors = errors;
        return false;
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
        // Use writeTypedList (paired with createTypedArrayList in our CREATOR):
        // the element type is known from TangramPiece.CREATOR, so this omits
        // the redundant per-element class name that writeParcelableArray adds.
        dest.writeTypedList(pieces);
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
     * the sizes are only advisory.  But to keep this operation simple,
     * the pieces must be in the same order.
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
            return pieces.equals(p2.pieces);

        }
        return false;
    }

    /**
     * Snap a given puzzle piece to any neighbors or to the puzzle grid,
     * according to the first match of the following rules:
     * <ol>
     *     <li>If this is the only piece in the play area <i>or</i>
     *     none of its edges is touching a nearly collinear edge of
     *     any other piece and none of its vertices is touching an
     *     edge or vertex of any other piece and none of its edges
     *     is touching a vertex of any other piece, then snap its
     *     rotation to the nearest 45&deg; and its position to
     *     the integer grid.</li>
     *     <li>If any edge of a piece touches the edge of another piece
     *     and they are nearly parallel (within specified tolerances),
     *     snap its rotation to make those edges parallel and its position
     *     to make them collinear.
     *     <ol>
     *         <li>If there is a second edge of the piece which is close
     *         to the parallel edge of another piece (within tolerance),
     *         snap its position in the direction parallel to the first
     *         edge to that the second edges are collinear.</li>
     *         <li>If there is a vertex of the piece which is close to
     *         either the edge or a vertex of another piece (within
     *         tolerance), snap the position in the direction parallel
     *         to the first edge such that the closest such vertex lies
     *         on the edge or vertex of the other piece.</li>
     *         <li>If there is another edge of the piece which is close to
     *         a vertex of another piece (within tolerance), snap the
     *         position in the direction parallel to the first edge
     *         such that the closest such edge passes through the vertex
     *         of the other piece.</li>
     *         <li>If one of the endpoints or the center of the snapped
     *         edge is close to one of the endpoints or center of the edge
     *         of the other piece that this was snapped to (within tolerance),
     *         snap the position in the direction parallel to the edges
     *         to make the nearest such points match.</li>
     *     </ol>
     *     </li>
     *     <li>If any vertex of a piece is close to an edge or vertex of
     *     another piece (within tolerance), snap its rotation to the
     *     nearest multiple of 15°.
     *     <ol>
     *         <li>If the vertex is close to either the vertex or midpoint
     *         on the edge of the other piece (within tolerance), snap its
     *         position such that the vertex matches the other point.</li>
     *         <li>Otherwise snap its position such that the vertex lies
     *         on the edge of the other piece.</li>
     *     </ol></li>
     * </ol>
     *
     * All other pieces in the puzzle are considered.  If the given piece is
     * not already in the puzzle, it is added.
     *
     * @param moved the piece to snap into place
     */
    public void snap(TangramPiece moved) {
        // Temporarily move the piece out of the list while
        // we consider whether it's touching the others.
        pieces.remove(moved);

        EdgeContact contact =
                pieces.isEmpty() ? null : findBestEdgeContact(moved);
        if (contact != null) {
            // Rules 2, 2.1-2.4: align to the touching neighbor edge.
            snapToEdge(moved, contact);
        } else if (pieces.isEmpty() || !snapToVertex(moved)) {
            // Rule 1: nothing to touch → snap to the grid and 45°.
            snapFree(moved);
        }
        // else: rule 3 (a vertex contact) was handled by snapToVertex.

        pieces.add(moved);
        Log.d(LOG_TAG, "Snapped " + moved);
    }

    /**
     * Rule 1: snap the piece&rsquo;s rotation to the nearest 45&deg; and its
     * centroid to the integer puzzle grid.
     */
    private void snapFree(@NonNull TangramPiece moved) {
        moved.setRotation(45 * Math.round(moved.getRotation() / 45f));
        moved.setPosition(moved.getPosition().nearestZGridPoint());
    }

    /**
     * Snap rule 3: when the moved piece isn&rsquo;t edge-to-edge with any
     * neighbor but one of its vertices is within {@link #POINT_PROXIMITY} of a
     * neighbor edge or vertex, snap its rotation to the nearest 15&deg; (e.g.
     * the Candle&rsquo;s tip connections) and its position so that vertex
     * lands on the nearest neighbor vertex or edge midpoint (rule 3.1) or,
     * failing that, onto the nearest neighbor edge (rule 3.2).
     *
     * @param moved the piece being snapped
     * @return {@code true} if a vertex contact was found and snapped,
     * {@code false} if the piece isn&rsquo;t vertex-touching any neighbor
     */
    private boolean snapToVertex(@NonNull TangramPiece moved) {
        // Which of the moved piece's vertices is the (closest) contact?
        TPoint[] movedVertices = moved.getVertices();
        int contactIndex = -1;
        double bestDistance = POINT_PROXIMITY;
        for (int i = 0; i < movedVertices.length; i++) {
            double d = distanceToNearestFeature(movedVertices[i]);
            if (d <= bestDistance) {
                bestDistance = d;
                contactIndex = i;
            }
        }
        if (contactIndex < 0)
            return false;

        // Rotation → nearest 15°.
        moved.setRotation(15 * Math.round(moved.getRotation() / 15f));

        // Snap the contact vertex to the nearest neighbor vertex/midpoint
        // (rule 3.1), or onto the nearest neighbor edge (rule 3.2).
        TPoint vertex = moved.getVertices()[contactIndex];
        TPoint target = nearestVertexOrMidpoint(vertex);
        if (target == null)
            target = nearestPointOnAnEdge(vertex);
        if (target != null)
            moved.setPosition(translated(moved.getPosition(),
                    coefficientDifference(target, vertex)));
        return true;
    }

    /**
     * @param p the point to measure from
     * @return the distance from {@code p} to the nearest neighbor edge or
     * vertex (a segment distance, so edges include their endpoints)
     */
    private double distanceToNearestFeature(@NonNull TPoint p) {
        double best = Double.POSITIVE_INFINITY;
        for (TangramPiece other : pieces) {
            for (TPoint w : other.getVertices())
                best = Math.min(best, p.distanceTo(w));
            for (TEdge edge : other.getEdges())
                best = Math.min(best, edge.distanceToPoint(p));
        }
        return best;
    }

    /**
     * @param p the point to snap
     * @return the nearest neighbor vertex or edge midpoint to {@code p} within
     * {@link #POINT_PROXIMITY}, or {@code null} if none is that close
     */
    @Nullable
    private TPoint nearestVertexOrMidpoint(@NonNull TPoint p) {
        TPoint best = null;
        double bestDistance = POINT_PROXIMITY;
        for (TangramPiece other : pieces) {
            for (TPoint w : other.getVertices()) {
                double d = p.distanceTo(w);
                if (d <= bestDistance) {
                    bestDistance = d;
                    best = w;
                }
            }
            for (TEdge edge : other.getEdges()) {
                TPoint m = edge.midpoint();
                double d = p.distanceTo(m);
                if (d <= bestDistance) {
                    bestDistance = d;
                    best = m;
                }
            }
        }
        return best;
    }

    /**
     * @param p the point to snap
     * @return the nearest point on any neighbor edge to {@code p} within
     * {@link #POINT_PROXIMITY}, or {@code null} if none is that close
     */
    @Nullable
    private TPoint nearestPointOnAnEdge(@NonNull TPoint p) {
        TPoint best = null;
        double bestDistance = POINT_PROXIMITY;
        for (TangramPiece other : pieces) {
            for (TEdge edge : other.getEdges()) {
                double d = edge.distanceToPoint(p);
                if (d <= bestDistance) {
                    bestDistance = d;
                    best = edge.nearestPointTo(p);
                }
            }
        }
        return best;
    }

    /**
     * Find the neighbor edge the moved piece is most in contact with: one of
     * the moved piece&rsquo;s edges that is nearly parallel to it (within
     * {@link #EDGE_ANGLE_TOLERANCE}), within {@link #EDGE_PROXIMITY}
     * perpendicular distance, and overlapping it by more than
     * {@link #EDGE_PROXIMITY} along its length.  (A smaller overlap is an
     * end-to-end/vertex contact, handled elsewhere.)
     *
     * @param moved the piece being snapped
     * @return the closest such contact (smallest perpendicular gap), or
     * {@code null} if no edge is touching
     */
    @Nullable
    private EdgeContact findBestEdgeContact(@NonNull TangramPiece moved) {
        TEdge[] movedEdges = moved.getEdges();
        EdgeContact best = null;
        for (int i = 0; i < movedEdges.length; i++) {
            TEdge movedEdge = movedEdges[i];
            for (TangramPiece other : pieces) {
                for (TEdge otherEdge : other.getEdges()) {
                    if (!movedEdge.isNearlyParallel(
                            otherEdge, EDGE_ANGLE_TOLERANCE))
                        continue;
                    double perp = otherEdge.perpendicularDistanceToLine(
                            movedEdge.midpoint());
                    if (perp > EDGE_PROXIMITY)
                        continue;
                    double overlap = otherEdge.overlapLength(movedEdge);
                    if (overlap <= EDGE_PROXIMITY)
                        continue; // end-to-end: a vertex contact, not an edge
                    if (best == null || perp < best.perpDistance
                            || (perp == best.perpDistance
                                    && overlap > best.overlap))
                        best = new EdgeContact(i, otherEdge, perp, overlap);
                }
            }
        }
        return best;
    }

    /**
     * Snap rules 2, 2.1&ndash;2.3, and 2.4 for a touching edge: rotate the
     * moved piece so its contact edge is parallel to the neighbor edge
     * (snapped to the nearest 45&deg;, since all edges in a valid tangram lie
     * at 45&deg; multiples), which leaves one degree of freedom&mdash;sliding
     * along the edge.  A second contact fixes that slide, in precedence order:
     * rule 2.1 (a second moved edge made collinear with a neighbor edge at a
     * different angle), 2.2 (a moved vertex brought onto a neighbor vertex or
     * edge), or 2.3 (another moved edge made to pass through a neighbor
     * vertex).  With no second contact, rule 2.4 aligns the nearest matching
     * endpoint/midpoint pair of the two edges exactly (or, failing that, just
     * makes the edges collinear).
     *
     * @param moved the piece being snapped
     * @param contact the edge contact found by {@link #findBestEdgeContact}
     */
    private void snapToEdge(@NonNull TangramPiece moved,
                            @NonNull EdgeContact contact) {
        TEdge neighbor = contact.neighborEdge;

        // 1. Rotate so the contact edge is parallel to the neighbor edge,
        //    then round to the nearest 45 degrees.
        TEdge movedEdge = moved.getEdges()[contact.movedEdgeIndex];
        double diff = neighbor.angleDegrees() - movedEdge.angleDegrees();
        diff -= 180 * Math.round(diff / 180.0); // fold to nearest parallel
        double parallel = moved.getRotation() + diff;
        moved.setRotation(45f * Math.round(parallel / 45.0));

        // 2. Re-read the contact edge in its new orientation, and compute the
        //    perpendicular shift that makes it collinear with the neighbor
        //    edge (this leaves the along-edge position untouched).
        movedEdge = moved.getEdges()[contact.movedEdgeIndex];
        TPoint mid = movedEdge.midpoint();
        TPoint perpDelta = coefficientDifference(
                neighbor.pointAtFraction(neighbor.projectionFraction(mid)), mid);

        // 3. Rules 2.1-2.3: a second contact fixes the remaining along-edge
        //    slide.  (Value is a signed distance along the neighbor edge.)
        Double slide = secondaryAlongSlide(
                moved, contact.movedEdgeIndex, neighbor, perpDelta);
        if (slide != null) {
            double length = neighbor.length();
            double ux = neighbor.getDx() / length, uy = neighbor.getDy() / length;
            TPoint delta = new ImmutableTPoint(
                    perpDelta.getXa() + (float) (slide * ux), perpDelta.getXb(),
                    perpDelta.getYa() + (float) (slide * uy), perpDelta.getYb());
            moved.setPosition(translated(moved.getPosition(), delta));
            return;
        }

        // 4. Rule 2.4: align the closest matching endpoint/midpoint pair of
        //    the two edges, measured along the edge.
        TPoint[] movedFeatures = {
                movedEdge.getStart(), movedEdge.getEnd(), mid};
        TPoint[] neighborFeatures = {
                neighbor.getStart(), neighbor.getEnd(), neighbor.midpoint()};
        TPoint fromFeature = null, toFeature = null;
        double bestSeparation = POINT_PROXIMITY;
        for (TPoint from : movedFeatures) {
            for (TPoint to : neighborFeatures) {
                double separation = alongEdgeSeparation(from, to, neighbor);
                if (separation <= bestSeparation) {
                    bestSeparation = separation;
                    fromFeature = from;
                    toFeature = to;
                }
            }
        }

        // Move the matched feature exactly onto the neighbor's feature (with
        // the edges parallel, sharing one point makes the lines collinear).
        // Working in coefficient space keeps the result exact in Q[√2] (the
        // piece's drag position cancels out).  With no feature in range, fall
        // back to the perpendicular-only collinear shift.
        TPoint delta = (fromFeature != null)
                ? coefficientDifference(toFeature, fromFeature)
                : perpDelta;
        moved.setPosition(translated(moved.getPosition(), delta));
    }

    /**
     * Determine the along-edge slide (a signed distance along {@code en}) that
     * satisfies a second contact, in precedence order of snap rules 2.1, 2.2,
     * and 2.3, given that the piece will first be shifted by {@code perpDelta}
     * to make its primary edge collinear with {@code en}.
     *
     * @param moved the piece being snapped
     * @param primaryIndex the index of the moved piece's primary contact edge
     * @param en the neighbor edge the primary edge is snapping to
     * @param perpDelta the perpendicular shift already determined for the
     *                  primary edge (accounted for when solving each slide)
     * @return the along-edge slide, or {@code null} if there is no second
     * contact within tolerance
     */
    @Nullable
    private Double secondaryAlongSlide(@NonNull TangramPiece moved,
            int primaryIndex, @NonNull TEdge en, @NonNull TPoint perpDelta) {
        double length = en.length();
        if (length == 0)
            return null;
        double ux = en.getDx() / length, uy = en.getDy() / length; // along E_n
        double nx = -uy, ny = ux;                                   // across E_n
        TEdge[] movedEdges = moved.getEdges();
        TPoint[] movedVertices = moved.getVertices();

        // Rule 2.1: a second moved edge parallel to (and touching) a neighbor
        // edge at a DIFFERENT angle -> slide so they become collinear.
        Double slide = null;
        double bestKey = Double.POSITIVE_INFINITY;
        for (int j = 0; j < movedEdges.length; j++) {
            if (j == primaryIndex)
                continue;
            TEdge movedEdge2 = movedEdges[j];
            for (TangramPiece other : pieces) {
                for (TEdge otherEdge2 : other.getEdges()) {
                    if (otherEdge2.isNearlyParallel(en, EDGE_ANGLE_TOLERANCE))
                        continue; // same angle as primary: can't slide to it
                    if (!movedEdge2.isNearlyParallel(
                            otherEdge2, EDGE_ANGLE_TOLERANCE))
                        continue;
                    double gap = otherEdge2.perpendicularDistanceToLine(
                            movedEdge2.midpoint());
                    if (gap > EDGE_PROXIMITY)
                        continue;
                    // Accept overlap OR near end-to-end contact (at a corner
                    // the second edges are collinear but don't overlap); only
                    // reject edges too far apart along their direction.
                    if (otherEdge2.overlapLength(movedEdge2) <= -EDGE_PROXIMITY)
                        continue;
                    double s = slideOntoLine(
                            movedEdge2.midpoint(), otherEdge2, ux, uy, perpDelta);
                    if (!Double.isNaN(s) && gap < bestKey) {
                        bestKey = gap;
                        slide = s;
                    }
                }
            }
        }
        if (slide != null)
            return slide;

        // Rule 2.2: a moved vertex close to a neighbor vertex or edge.
        // (bestKey is still +infinity: rule 2.1 only lowered it together with
        // setting slide, and a non-null slide returned above.)
        double perpShift = perpDelta.getX() * nx + perpDelta.getY() * ny;
        double alongShift = perpDelta.getX() * ux + perpDelta.getY() * uy;
        for (TPoint v : movedVertices) {
            double vAlong = v.getX() * ux + v.getY() * uy + alongShift;
            double vPerp = v.getX() * nx + v.getY() * ny + perpShift;
            for (TangramPiece other : pieces) {
                for (TPoint w : other.getVertices()) {
                    // Reachable by an along-slide only if already at the same
                    // level across the edge.
                    double perpMiss = Math.abs(
                            vPerp - (w.getX() * nx + w.getY() * ny));
                    if (perpMiss > POINT_PROXIMITY)
                        continue;
                    double s = (w.getX() * ux + w.getY() * uy) - vAlong;
                    if (Math.abs(s) <= POINT_PROXIMITY && perpMiss < bestKey) {
                        bestKey = perpMiss;
                        slide = s;
                    }
                }
                for (TEdge otherEdge2 : other.getEdges()) {
                    if (otherEdge2.isNearlyParallel(en, EDGE_ANGLE_TOLERANCE))
                        continue;
                    double gap = otherEdge2.distanceToPoint(v);
                    if (gap > POINT_PROXIMITY)
                        continue;
                    double s = slideOntoLine(v, otherEdge2, ux, uy, perpDelta);
                    if (!Double.isNaN(s) && Math.abs(s) <= POINT_PROXIMITY
                            && gap < bestKey) {
                        bestKey = gap;
                        slide = s;
                    }
                }
            }
        }
        if (slide != null)
            return slide;

        // Rule 2.3: another moved edge close to a neighbor vertex -> slide so
        // the edge passes through the vertex.  (bestKey is still +infinity, as
        // above.)
        for (int j = 0; j < movedEdges.length; j++) {
            if (j == primaryIndex)
                continue;
            TEdge movedEdge2 = movedEdges[j];
            if (movedEdge2.isNearlyParallel(en, EDGE_ANGLE_TOLERANCE))
                continue;
            for (TangramPiece other : pieces) {
                for (TPoint w : other.getVertices()) {
                    double gap = movedEdge2.distanceToPoint(w);
                    if (gap > POINT_PROXIMITY)
                        continue;
                    double s = slideEdgeThroughPoint(
                            movedEdge2, w, ux, uy, perpDelta);
                    if (!Double.isNaN(s) && Math.abs(s) <= POINT_PROXIMITY
                            && gap < bestKey) {
                        bestKey = gap;
                        slide = s;
                    }
                }
            }
        }
        return slide;
    }

    /**
     * @return the signed distance to slide {@code point} along the direction
     * ({@code ux},&nbsp;{@code uy}) so that, after also applying
     * {@code perpDelta}, it lies on {@code line}&rsquo;s infinite line, or NaN
     * if the slide direction is parallel to the line.
     */
    private static double slideOntoLine(@NonNull TPoint point,
            @NonNull TEdge line, double ux, double uy, @NonNull TPoint perpDelta) {
        double length = line.length();
        if (length == 0)
            return Double.NaN;
        double nx = -line.getDy() / length, ny = line.getDx() / length;
        double denominator = ux * nx + uy * ny;
        if (Math.abs(denominator) < 1e-6)
            return Double.NaN;
        double signedDistance =
                (point.getX() - line.getStart().getX()) * nx
                        + (point.getY() - line.getStart().getY()) * ny;
        double perpComponent = perpDelta.getX() * nx + perpDelta.getY() * ny;
        return -(signedDistance + perpComponent) / denominator;
    }

    /**
     * @return the signed distance to slide the moved piece along
     * ({@code ux},&nbsp;{@code uy}) so that {@code edge} (after also applying
     * {@code perpDelta}) passes through the fixed neighbor {@code vertex}, or
     * NaN if the slide direction is parallel to the edge.
     */
    private static double slideEdgeThroughPoint(@NonNull TEdge edge,
            @NonNull TPoint vertex, double ux, double uy,
            @NonNull TPoint perpDelta) {
        double length = edge.length();
        if (length == 0)
            return Double.NaN;
        double nx = -edge.getDy() / length, ny = edge.getDx() / length;
        double denominator = ux * nx + uy * ny;
        if (Math.abs(denominator) < 1e-6)
            return Double.NaN;
        double signedDistance =
                (vertex.getX() - edge.getStart().getX()) * nx
                        + (vertex.getY() - edge.getStart().getY()) * ny;
        double perpComponent = perpDelta.getX() * nx + perpDelta.getY() * ny;
        return (signedDistance - perpComponent) / denominator;
    }

    /**
     * @param from a point on the moved edge
     * @param to a point on the neighbor edge
     * @param edge the neighbor edge whose direction to measure along
     * @return how far apart {@code from} and {@code to} are measured parallel
     * to {@code edge} (their separation once brought onto the same line)
     */
    private static double alongEdgeSeparation(
            @NonNull TPoint from, @NonNull TPoint to, @NonNull TEdge edge) {
        double length = edge.length();
        if (length == 0)
            return from.distanceTo(to);
        double ux = edge.getDx() / length, uy = edge.getDy() / length;
        double dx = from.getX() - to.getX(), dy = from.getY() - to.getY();
        return Math.abs(dx * ux + dy * uy);
    }

    /**
     * @return an exact {@code a - b} in coefficient space (a translation delta
     * that preserves the &#8474;[&#8730;2] form when both inputs are exact)
     */
    @NonNull
    private static TPoint coefficientDifference(
            @NonNull TPoint a, @NonNull TPoint b) {
        return new ImmutableTPoint(a.getXa() - b.getXa(), a.getXb() - b.getXb(),
                a.getYa() - b.getYa(), a.getYb() - b.getYb());
    }

    /**
     * @return a new immutable point at {@code origin + delta} (coefficient-
     * wise).  A fresh object is always returned so the piece&rsquo;s vertex
     * cache is correctly invalidated even if {@code origin} is mutable.
     */
    @NonNull
    private static TPoint translated(@NonNull TPoint origin, @NonNull TPoint delta) {
        return new ImmutableTPoint(
                origin.getXa() + delta.getXa(), origin.getXb() + delta.getXb(),
                origin.getYa() + delta.getYa(), origin.getYb() + delta.getYb());
    }

    /** A detected edge-to-edge contact between the moved piece and a neighbor. */
    private static class EdgeContact {
        final int movedEdgeIndex;
        final TEdge neighborEdge;
        final double perpDistance;
        final double overlap;

        EdgeContact(int movedEdgeIndex, TEdge neighborEdge,
                    double perpDistance, double overlap) {
            this.movedEdgeIndex = movedEdgeIndex;
            this.neighborEdge = neighborEdge;
            this.perpDistance = perpDistance;
            this.overlap = overlap;
        }
    }

    /**
     * Find the point at the center of the puzzle.  This walks through
     * all of the pieces&rsquo;s vertices looking for the minimum and
     * maximum values of each coordinate, then gets their average.
     * If the puzzle is currently empty, this returns the origin.
     * At the same time, this also re-calculates the {@code size} of
     * the puzzle (provided it is not empty).
     *
     * @return the center point of the puzzle
     */
    public @NonNull TPoint getCenter() {
        if (pieces.isEmpty())
            return ImmutableTPoint.ORIGIN;
        float minXa = Float.POSITIVE_INFINITY;
        float maxXa = Float.NEGATIVE_INFINITY;
        float minXb = Float.POSITIVE_INFINITY;
        float maxXb = Float.NEGATIVE_INFINITY;
        float minYa = Float.POSITIVE_INFINITY;
        float maxYa = Float.NEGATIVE_INFINITY;
        float minYb = Float.POSITIVE_INFINITY;
        float maxYb = Float.NEGATIVE_INFINITY;
        for (TangramPiece piece : pieces) {
            for (TPoint vertex : piece.getVertices()) {
                if (vertex.getXa() < minXa)
                    minXa = vertex.getXa();
                if (vertex.getXa() > maxXa)
                    maxXa = vertex.getXa();
                if (vertex.getXb() < minXb)
                    minXb = vertex.getXb();
                if (vertex.getXb() > maxXb)
                    maxXb = vertex.getXb();
                if (vertex.getYa() < minYa)
                    minYa = vertex.getYa();
                if (vertex.getYa() > maxYa)
                    maxYa = vertex.getYa();
                if (vertex.getYb() < minYb)
                    minYb = vertex.getYb();
                if (vertex.getYb() > maxYb)
                    maxYb = vertex.getYb();
            }
        }
        size = (float) Math.max(maxXa - minXa + (maxXb- minXb) * TPoint.SQRT2,
                maxYa - minYa + (maxYb - minYb) * TPoint.SQRT2);
        return new ImmutableTPoint((minXa + maxXa) / 2, (minXb + maxXb) / 2,
                (minYa + maxYa) / 2, (minYb + maxYb) / 2);
    }

}
