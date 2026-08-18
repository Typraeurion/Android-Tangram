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
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The filled region covered by a group of Tangram pieces, used to compare a
 * player&rsquo;s arrangement against a goal silhouette.
 *
 * <p>The region is built by accumulating pieces with {@link
 * #merge(TangramPiece)} (or {@link #fromPuzzle(TangramPuzzle)}) and then
 * reduced to its boundary: shared interior edges cancel and collinear runs
 * merge into maximal edges, so the result is independent of <em>how</em> the
 * region was tiled (a large triangle vs. a medium&nbsp;+&nbsp;two small
 * triangles yield the same boundary).  A region may have holes (e.g. the
 * pinwheel), so it is kept as a set of boundary loops&mdash;one outer loop
 * plus one loop per hole&mdash;not a single closed path.</p>
 *
 * <p>{@link #isIdenticalTo(TPolygon)} compares two regions by edge angle and
 * length within {@link #EDGE_ANGLE_TOLERANCE}/{@link #EDGE_LENGTH_TOLERANCE}
 * rather than by exact vertex positions, which absorbs both &#8474;[&#8730;2]
 * grid drift and interior vertices that land partway along a neighbor&rsquo;s
 * edge.  It is translation-invariant (position does not matter) but
 * <em>not</em> rotation- or reflection-invariant.</p>
 *
 * @author Trevin Beattie
 * @author Claude Opus 4.8
 */
public class TPolygon implements Parcelable {

    /**
     * Maximum difference, in puzzle units, for two edge lengths
     * to be considered identical.
     */
    public static final double EDGE_LENGTH_TOLERANCE = 1.0;

    /**
     * Maximum difference in degrees for two
     * angles to be considered identical.
     */
    public static final double EDGE_ANGLE_TOLERANCE = 5.0;

    /**
     * Tolerance used while <em>building</em> the boundary (grouping edges
     * onto a common line, deduplicating breakpoints, and joining segments
     * into loops).  This only needs to absorb the small gaps a valid,
     * connected arrangement may have between touching pieces, so it is much
     * tighter than {@link #EDGE_LENGTH_TOLERANCE} (which is the tolerance for
     * <em>comparing</em> two finished regions).
     */
    private static final double BUILD_TOLERANCE = 0.1;

    /**
     * The pieces&rsquo; edges accumulated by {@link #merge(TangramPiece)},
     * each directed with a consistent winding so that shared interior edges
     * run in opposite directions.  The boundary loops are derived from these
     * lazily by {@link #build()}.
     */
    private final List<TEdge> edges = new ArrayList<>();

    /** The region&rsquo;s boundary loops, or {@code null} until built. */
    @Nullable
    private List<Loop> loops;

    /**
     * Create an empty region; add pieces with {@link #merge(TangramPiece)}.
     */
    public TPolygon() {
    }

    /**
     * Construct a polygon from a {@link Parcel}.
     */
    private TPolygon(Parcel in) {
        in.readTypedList(edges, TEdge.CREATOR);
    }

    /**
     * Build a region from every piece of a puzzle.
     *
     * @param puzzle the puzzle whose pieces to cover
     * @return the region the puzzle&rsquo;s pieces fill
     */
    @NonNull
    public static TPolygon fromPuzzle(@NonNull TangramPuzzle puzzle) {
        TPolygon polygon = new TPolygon();
        for (TangramPiece piece : puzzle.getPieces())
            polygon.merge(piece);
        return polygon;
    }

    /**
     * Add a piece&rsquo;s area to the region.  The piece&rsquo;s edges are
     * stored with a consistent winding (normalized by signed area, which also
     * handles a mirrored parallelogram); the actual boundary is computed
     * later by {@link #build()}.
     *
     * @param piece the piece to merge
     */
    public void merge(@NonNull TangramPiece piece) {
        TPoint[] vertices = piece.getVertices();
        int n = vertices.length;
        // Signed area (shoelace) to normalize the winding direction so that
        // every piece is wound the same way.
        double area2 = 0;
        for (int i = 0; i < n; i++) {
            TPoint a = vertices[i], b = vertices[(i + 1) % n];
            area2 += a.getX() * b.getY() - b.getX() * a.getY();
        }
        boolean forward = area2 >= 0;
        for (int i = 0; i < n; i++) {
            TPoint a = vertices[i], b = vertices[(i + 1) % n];
            edges.add(forward ? new TEdge(a, b) : new TEdge(b, a));
        }
        loops = null; // invalidate any previously-built boundary
    }

    /**
     * Compare this region with another for having the same shape (same outer
     * outline and same holes in the same relative places), comparing edge
     * angles and lengths within tolerance rather than exact vertex positions.
     * Translation-invariant; not rotation- or reflection-invariant.
     *
     * @param poly2 the region to compare against
     * @return {@code true} if the two regions have the same shape
     */
    public boolean isIdenticalTo(@NonNull TPolygon poly2) {
        build();
        poly2.build();
        if (loops.size() != poly2.loops.size())
            return false;
        if (loops.isEmpty())
            return true;

        Loop outer1 = largestLoop(loops);
        Loop outer2 = largestLoop(poly2.loops);
        if (!loopsMatch(outer1, outer2))
            return false;

        // Match each hole to an unused hole of the same shape whose position
        // relative to the outer outline's anchor agrees within tolerance.
        List<Loop> holes2 = new ArrayList<>(poly2.loops);
        holes2.remove(outer2);
        for (Loop hole1 : loops) {
            if (hole1 == outer1)
                continue;
            double dx1 = hole1.anchorX - outer1.anchorX;
            double dy1 = hole1.anchorY - outer1.anchorY;
            boolean matched = false;
            for (int j = 0; j < holes2.size(); j++) {
                Loop hole2 = holes2.get(j);
                if (!loopsMatch(hole1, hole2))
                    continue;
                double dx2 = hole2.anchorX - outer2.anchorX;
                double dy2 = hole2.anchorY - outer2.anchorY;
                if (vectorsMatch(dx1, dy1, dx2, dy2)) {
                    holes2.remove(j);
                    matched = true;
                    break;
                }
            }
            if (!matched)
                return false;
        }
        return true;
    }

    /** @return the number of boundary loops (one outer plus one per hole). */
    int loopCount() {
        build();
        return loops.size();
    }

    /** @return a human-readable dump of the loops (diagnostic/test hook). */
    @NonNull
    String describeLoops() {
        build();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < loops.size(); i++) {
            Loop loop = loops.get(i);
            sb.append(String.format(java.util.Locale.US,
                    "loop %d area=%.2f anchor=(%.2f,%.2f) %d edges: ",
                    i, loop.area, loop.anchorX, loop.anchorY, loop.angles.length));
            for (TEdge e : loop.loopEdges)
                sb.append(String.format(java.util.Locale.US, "(%.2f,%.2f)->",
                        e.getStart().getX(), e.getStart().getY()));
            sb.append('\n');
        }
        return sb.toString();
    }

    // ---- boundary construction --------------------------------------------

    /**
     * Reduce the accumulated piece edges to the region&rsquo;s boundary
     * loops, if not already done.  Interior edges (shared by two pieces)
     * cancel, collinear runs merge into maximal edges, and the survivors are
     * joined into closed loops.
     *
     * <p>This is done lazily by {@link #isIdenticalTo(TPolygon)}, but may be
     * called explicitly to precompute the (potentially costly) loops of a
     * region that will be compared repeatedly&mdash;e.g. a static goal.  The
     * result is cached, so further {@link #merge(TangramPiece)} calls would
     * force a rebuild on the next use.</p>
     */
    public void build() {
        if (loops != null)
            return;
        // Group edges onto common supporting lines.
        List<LineBucket> lines = new ArrayList<>();
        for (TEdge edge : edges) {
            if (edge.length() < BUILD_TOLERANCE)
                continue;
            LineBucket line = null;
            for (LineBucket candidate : lines) {
                if (candidate.matches(edge)) {
                    line = candidate;
                    break;
                }
            }
            if (line == null) {
                line = new LineBucket(edge);
                lines.add(line);
            } else {
                line.add(edge);
            }
        }
        // Each line contributes its uncancelled, merged boundary segments.
        List<TEdge> boundary = new ArrayList<>();
        for (LineBucket line : lines)
            line.appendBoundary(boundary);
        loops = assembleLoops(boundary);
    }

    /**
     * Join directed boundary segments head-to-tail into closed loops.
     *
     * @param segments the boundary segments (each directed)
     * @return the closed loops formed
     */
    @NonNull
    private static List<Loop> assembleLoops(@NonNull List<TEdge> segments) {
        List<Loop> result = new ArrayList<>();
        boolean[] used = new boolean[segments.size()];
        for (int start = 0; start < segments.size(); start++) {
            if (used[start])
                continue;
            List<TEdge> loop = new ArrayList<>();
            int current = start;
            while (current >= 0 && !used[current]) {
                used[current] = true;
                loop.add(segments.get(current));
                current = findNext(segments, used, segments.get(current));
            }
            if (loop.size() >= 3)
                result.add(new Loop(loop));
        }
        return result;
    }

    /**
     * Find the unused segment that continues the loop from the given
     * segment&rsquo;s end.  Where several segments meet (a point the boundary
     * touches itself, e.g. the waist of a figure-8), the correct continuation
     * is the one making the sharpest left turn, which keeps the region on a
     * consistent side and traces self-touching outlines canonically&mdash;so
     * congruent shapes always yield the same loops.
     *
     * @return the index of the next segment, or -1 if none continues the loop
     */
    private static int findNext(@NonNull List<TEdge> segments,
                                @NonNull boolean[] used, @NonNull TEdge from) {
        double ex = from.getEnd().getX(), ey = from.getEnd().getY();
        double inAngle = from.angleDegrees();
        int best = -1;
        double bestTurn = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < segments.size(); i++) {
            if (used[i])
                continue;
            TEdge candidate = segments.get(i);
            double sx = candidate.getStart().getX();
            double sy = candidate.getStart().getY();
            if (Math.hypot(sx - ex, sy - ey) > BUILD_TOLERANCE)
                continue;
            double turn = signedTurn(inAngle, candidate.angleDegrees());
            if (turn > bestTurn) {
                bestTurn = turn;
                best = i;
            }
        }
        return best;
    }

    /**
     * @return the turn from heading {@code fromDeg} to heading {@code toDeg},
     * in degrees within (-180, 180]; positive is a left turn
     */
    private static double signedTurn(double fromDeg, double toDeg) {
        double d = (toDeg - fromDeg) % 360.0;
        if (d <= -180.0)
            d += 360.0;
        if (d > 180.0)
            d -= 360.0;
        return d;
    }

    // ---- comparison helpers -----------------------------------------------

    @NonNull
    private static Loop largestLoop(@NonNull List<Loop> loops) {
        Loop largest = loops.get(0);
        for (Loop loop : loops)
            if (Math.abs(loop.area) > Math.abs(largest.area))
                largest = loop;
        return largest;
    }

    /**
     * @return whether two loops have the same cyclic sequence of edge angles
     * and lengths, within tolerance, at some rotational offset
     */
    private static boolean loopsMatch(@NonNull Loop a, @NonNull Loop b) {
        int n = a.angles.length;
        if (b.angles.length != n)
            return false;
        for (int offset = 0; offset < n; offset++) {
            boolean ok = true;
            for (int i = 0; i < n; i++) {
                int j = (i + offset) % n;
                if (Math.abs(a.lengths[i] - b.lengths[j]) > EDGE_LENGTH_TOLERANCE
                        || angleDifference(a.angles[i], b.angles[j])
                                > EDGE_ANGLE_TOLERANCE) {
                    ok = false;
                    break;
                }
            }
            if (ok)
                return true;
        }
        return false;
    }

    /**
     * @return whether two offset vectors match in length and direction within
     * tolerance
     */
    private static boolean vectorsMatch(double ax, double ay,
                                        double bx, double by) {
        if (Math.abs(Math.hypot(ax, ay) - Math.hypot(bx, by))
                > EDGE_LENGTH_TOLERANCE)
            return false;
        double angleA = Math.toDegrees(Math.atan2(ay, ax));
        double angleB = Math.toDegrees(Math.atan2(by, bx));
        return angleDifference(angleA, angleB) <= EDGE_ANGLE_TOLERANCE;
    }

    /** @return the smallest absolute difference between two angles (degrees). */
    private static double angleDifference(double a, double b) {
        double diff = Math.abs(a - b) % 360.0;
        return (diff > 180.0) ? 360.0 - diff : diff;
    }

    /** @return {@code angle} folded into [0, 180). */
    private static double mod180(double angle) {
        double a = angle % 180.0;
        return (a < 0) ? a + 180.0 : a;
    }

    // ---- helper types -----------------------------------------------------

    /** A closed boundary loop, precomputed for comparison. */
    private static final class Loop {
        final List<TEdge> loopEdges;
        final double[] angles;
        final double[] lengths;
        final double area;      // signed (shoelace); sign follows winding
        final double anchorX;   // an extreme (min-y, then min-x) vertex,
        final double anchorY;   // a true corner, stable across tilings

        Loop(@NonNull List<TEdge> loopEdges) {
            this.loopEdges = loopEdges;
            int n = loopEdges.size();
            angles = new double[n];
            lengths = new double[n];
            double signedArea = 0;
            double ax = Double.POSITIVE_INFINITY, ay = Double.POSITIVE_INFINITY;
            for (int i = 0; i < n; i++) {
                TEdge e = loopEdges.get(i);
                angles[i] = e.angleDegrees();
                lengths[i] = e.length();
                double x = e.getStart().getX(), y = e.getStart().getY();
                TPoint end = e.getEnd();
                signedArea += x * end.getY() - end.getX() * y;
                if (y < ay || (y == ay && x < ax)) {
                    ax = x;
                    ay = y;
                }
            }
            area = signedArea / 2.0;
            anchorX = ax;
            anchorY = ay;
        }
    }

    /**
     * A set of collinear edges sharing one supporting line, from which the
     * uncancelled (boundary) portions are extracted by signed 1-D interval
     * arithmetic along the line.
     */
    private static final class LineBucket {
        private final double ux, uy;          // unit direction
        private final double px, py;          // a point on the line
        private final double lineAngle;       // [0, 180)
        // Each edge as {t0, t1, sign} projected onto the line direction.
        private final List<double[]> spans = new ArrayList<>();

        LineBucket(@NonNull TEdge edge) {
            double dx = edge.getDx(), dy = edge.getDy();
            double len = Math.hypot(dx, dy);
            ux = dx / len;
            uy = dy / len;
            px = edge.getStart().getX();
            py = edge.getStart().getY();
            lineAngle = mod180(Math.toDegrees(Math.atan2(dy, dx)));
            add(edge);
        }

        boolean matches(@NonNull TEdge edge) {
            double angle = mod180(Math.toDegrees(
                    Math.atan2(edge.getDy(), edge.getDx())));
            double d = Math.abs(angle - lineAngle);
            if (d > 90.0)
                d = 180.0 - d;
            if (d > EDGE_ANGLE_TOLERANCE)
                return false;
            // Perpendicular distance of the edge's start to this line.
            double nx = -uy, ny = ux;
            double perp = Math.abs(nx * (edge.getStart().getX() - px)
                    + ny * (edge.getStart().getY() - py));
            return perp <= BUILD_TOLERANCE;
        }

        void add(@NonNull TEdge edge) {
            double t0 = ux * (edge.getStart().getX() - px)
                    + uy * (edge.getStart().getY() - py);
            double t1 = ux * (edge.getEnd().getX() - px)
                    + uy * (edge.getEnd().getY() - py);
            double sign = (t1 >= t0) ? 1 : -1;
            spans.add(new double[] {Math.min(t0, t1), Math.max(t0, t1), sign});
        }

        /**
         * Emit the maximal boundary segments of this line (intervals where the
         * signed coverage is non-zero) into {@code out}, directed to follow
         * the region&rsquo;s winding.
         */
        void appendBoundary(@NonNull List<TEdge> out) {
            // Distinct breakpoints (interval ends), deduplicated.
            double[] cuts = new double[spans.size() * 2];
            for (int i = 0; i < spans.size(); i++) {
                cuts[i * 2] = spans.get(i)[0];
                cuts[i * 2 + 1] = spans.get(i)[1];
            }
            Arrays.sort(cuts);
            List<Double> breaks = new ArrayList<>();
            for (double c : cuts)
                if (breaks.isEmpty()
                        || c - breaks.get(breaks.size() - 1) > BUILD_TOLERANCE)
                    breaks.add(c);

            // Walk each elementary interval; accumulate a maximal run of a
            // constant non-zero coverage sign into one boundary segment.
            double runStart = Double.NaN;
            int runSign = 0;
            for (int i = 0; i + 1 < breaks.size(); i++) {
                double lo = breaks.get(i), hi = breaks.get(i + 1);
                double mid = (lo + hi) / 2.0;
                int net = 0;
                for (double[] span : spans)
                    if (span[0] <= mid && mid <= span[1])
                        net += (int) span[2];
                int sign = Integer.signum(net);
                if (sign != runSign) {
                    if (runSign != 0)
                        out.add(segment(runStart, lo, runSign));
                    runStart = lo;
                    runSign = sign;
                }
            }
            if (runSign != 0)
                out.add(segment(runStart, breaks.get(breaks.size() - 1),
                        runSign));
        }

        /** A directed boundary segment from the interval [a, b] with sign. */
        @NonNull
        private TEdge segment(double a, double b, int sign) {
            TPoint pa = pointAt(a), pb = pointAt(b);
            return (sign > 0) ? new TEdge(pa, pb) : new TEdge(pb, pa);
        }

        @NonNull
        private TPoint pointAt(double t) {
            return new ImmutableTPoint(
                    (float) (px + t * ux), 0f, (float) (py + t * uy), 0f);
        }
    }

    // ---- Parcelable & Object ----------------------------------------------

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedList(edges);
    }

    public static final Creator<TPolygon> CREATOR = new Creator<>() {
        @Override
        public TPolygon createFromParcel(Parcel in) {
            return new TPolygon(in);
        }

        @Override
        public TPolygon[] newArray(int size) {
            return new TPolygon[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public @NonNull String toString() {
        build();
        StringBuilder sb = new StringBuilder("TPolygon[");
        if (loops.isEmpty())
            return sb.append("empty]").toString();
        for (int i = 0; i < loops.size(); i++) {
            if (i > 0)
                sb.append("; ");
            sb.append(loops.get(i).angles.length).append(" edges");
        }
        return sb.append(']').toString();
    }
}