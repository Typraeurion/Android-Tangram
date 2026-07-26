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
package com.xmission.trevin.android.tangram.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.TPoint;
import com.xmission.trevin.android.tangram.data.TangramPiece;
import com.xmission.trevin.android.tangram.data.TangramPuzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This is the main area of the UI where the player can move the
 * pieces around to form a Tangram shape.
 *
 * <p>The view owns both the drawing and (eventually) the touch handling
 * for every piece rather than wrapping each piece in its own child view:
 * Android views are always axis-aligned rectangles for layout and touch
 * dispatch, which does not suit overlapping, rotated polygons.  Pieces
 * are drawn as paths built directly from
 * {@link TangramPiece#getVertices()} so that rendering and hit-testing
 * share a single source of truth for each piece&rsquo;s geometry.</p>
 */
public class PlayTableView extends View {

    /**
     * Reference to the current puzzle the player is trying to solve,
     * or {@code null} if the player is working in freestyle / sketch mode.
     */
    @Nullable
    TangramPuzzle solution;

    /** Paint used to fill the interior of each piece. */
    private final Paint fillPaint = new Paint();

    /** Paint used to stroke the outline of each piece. */
    private final Paint outlinePaint = new Paint();

    /** Reusable path for building a piece&rsquo;s outline in {@link #onDraw}. */
    private final Path piecePath = new Path();

    /** Reusable buffer for mapping a single point (avoids per-frame allocation). */
    private final float[] pointBuffer = new float[2];

    /**
     * Reusable buffer for mapping a piece&rsquo;s vertices, as interleaved
     * (x, y) pairs.  Grown on demand for pieces with more vertices.
     */
    private float[] vertexBuffer = new float[8];

    /** Maps puzzle coordinates to view (pixel) coordinates. */
    private final Matrix puzzleToView = new Matrix();

    /**
     * Inverse of {@link #puzzleToView}, for mapping touch points back
     * into puzzle space.  Kept up to date alongside {@link #puzzleToView}.
     */
    private final Matrix viewToPuzzle = new Matrix();

    /**
     * Display scale, in pixels per puzzle unit, at which the whole
     * puzzle area just fits within the view.  Recomputed whenever the
     * view is resized or the puzzle changes.  Zero means &ldquo;not yet
     * measured&rdquo;, in which case nothing is drawn.
     */
    private float fitScale = 0f;

    /**
     * User zoom multiplier applied on top of {@link #fitScale}, letting
     * the player resize the play table within
     * [{@link #MIN_ZOOM}, {@link #MAX_ZOOM}].  The effective scale is
     * {@link #getUnitScale()}.
     */
    private float userZoom = 1f;

    /** Fraction of the view kept as margin around the puzzle when fitting. */
    private static final float FIT_MARGIN = 0.95f;

    /** Smallest allowed value for {@link #userZoom}. */
    private static final float MIN_ZOOM = 0.5f;

    /** Largest allowed value for {@link #userZoom}. */
    private static final float MAX_ZOOM = 4.0f;

    /** Cached outline color from the current theme. */
    private int outlineColor = Color.BLACK;

    /** Cached outline width, in pixels, from the current theme. */
    private float outlineWidthPx = 0f;

    /** Cached fill colors from the current theme, keyed by color attribute. */
    private final SparseIntArray fillColors = new SparseIntArray();

    /**
     * The pieces this view owns, in back-to-front draw order: the last
     * element is on top and is the first candidate for hit-testing.  This
     * list is the view&rsquo;s authoritative state; a {@link TangramPuzzle}
     * can be built from it (or used to populate it) at the boundary.
     */
    private final List<TangramPiece> pieces = new ArrayList<>();

    /**
     * The playfield extent in puzzle units, used to fit the board to the
     * view.  Defaults to the standard puzzle size until {@link #setPuzzle}
     * (or a future configuration setter) overrides it.
     */
    private float playfieldSize = 36f;

    /** Reusable buffer for mapping a touch point into puzzle space. */
    private final float[] touchBuffer = new float[2];

    /** The piece currently grabbed by the player, or {@code null}. */
    @Nullable
    private TangramPiece selectedPiece;

    /**
     * The id of the pointer currently dragging {@link #selectedPiece},
     * or {@link MotionEvent#INVALID_POINTER_ID} when nothing is dragging.
     */
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;

    /**
     * Offset, in puzzle units, from the grabbing touch point to the
     * piece&rsquo;s centroid, so the piece keeps its grab point under the
     * finger while dragging.
     */
    private float grabOffsetX, grabOffsetY;

    public PlayTableView(Context context) {
        super(context);
        init();
    }

    public PlayTableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);
        outlinePaint.setAntiAlias(true);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeJoin(Paint.Join.MITER);
        refreshThemeCache();
    }

    /**
     * Load the pieces and playfield size from a puzzle into this view.
     * The view keeps its own references from here on; the puzzle is not
     * retained.  Recomputes the fit-to-view scale if the view has already
     * been laid out.
     *
     * @param puzzle the puzzle to load; must not be {@code null}
     */
    public void setPuzzle(TangramPuzzle puzzle) {
        playfieldSize = puzzle.getSize();
        pieces.clear();
        for (int i = 0; i < puzzle.getPieceCount(); i++)
            pieces.add(puzzle.getPiece(i));
        selectedPiece = null;
        activePointerId = MotionEvent.INVALID_POINTER_ID;
        computeFitScale();
        rebuildTransform();
        invalidate();
    }

    /**
     * Re-read the piece fill colors, outline color, and outline width
     * from the current theme.  Call this after switching play modes
     * (which swaps in a different theme) so the next {@link #onDraw}
     * picks up the new styling.
     */
    public void refreshThemeCache() {

        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                R.styleable.PlayTableView);
        try {
            outlineColor = a.getColor(
                    R.styleable.PlayTableView_tangramOutlineColor, Color.BLACK);
            outlineWidthPx = resolveDimension(a,
                    R.styleable.PlayTableView_tangramOutlineWidth);
            fillColors.clear();
            fillColors.put(R.attr.tangramSmallTriangleColor, a.getColor(
                    R.styleable.PlayTableView_tangramSmallTriangleColor, Color.GRAY));
            fillColors.put(R.attr.tangramSmallSquareColor, a.getColor(
                    R.styleable.PlayTableView_tangramSmallSquareColor, Color.GRAY));
            fillColors.put(R.attr.tangramParallelogramColor, a.getColor(
                    R.styleable.PlayTableView_tangramParallelogramColor, Color.GRAY));
            fillColors.put(R.attr.tangramMediumTriangleColor, a.getColor(
                    R.styleable.PlayTableView_tangramMediumTriangleColor, Color.GRAY));
            fillColors.put(R.attr.tangramLargeTriangleColor, a.getColor(
                    R.styleable.PlayTableView_tangramLargeTriangleColor, Color.GRAY));
        } finally {
            a.recycle();
        }
        invalidate();
    }

    /**
     * Resolve a theme value that may be authored either as a true
     * dimension (e.g. {@code 2dp}) or as a bare number (treated as dp).
     *
     * @return the value in pixels
     */
    private float resolveDimension(TypedArray a, @StyleableRes int index) {
        float density = getResources().getDisplayMetrics().density;
        TypedValue tv = a.peekValue(index);
        if (tv == null)
            return 0f;
        if (tv.type == TypedValue.TYPE_DIMENSION)
            return a.getDimension(index, 0f);
        if (tv.type == TypedValue.TYPE_FLOAT)
            return tv.getFloat() * density;
        if (tv.type >= TypedValue.TYPE_FIRST_INT && tv.type <= TypedValue.TYPE_LAST_INT)
            return tv.data * density;
        return 0f;
    }

    /** @return the current display scale in pixels per puzzle unit. */
    public float getUnitScale() {
        return fitScale * userZoom;
    }

    /** @return the current user zoom multiplier (relative to fit-to-view). */
    public float getZoom() {
        return userZoom;
    }

    /**
     * Resize the play table by setting the user zoom multiplier.
     * The value is clamped to [{@link #MIN_ZOOM}, {@link #MAX_ZOOM}].
     *
     * @param zoom the desired multiplier relative to the fit-to-view scale
     */
    public void setZoom(float zoom) {
        float clamped = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
        if (clamped != userZoom) {
            userZoom = clamped;
            rebuildTransform();
            invalidate();
        }
    }

    /**
     * Multiply the current zoom by a factor, e.g. from a pinch gesture.
     *
     * @param factor the multiplier to apply to the current zoom
     */
    public void zoomBy(float factor) {
        setZoom(userZoom * factor);
    }

    /**
     * Compute {@link #fitScale} so the puzzle&rsquo;s full extent fits
     * within the current view bounds (less {@link #FIT_MARGIN}).
     */
    private void computeFitScale() {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0)
            return;
        fitScale = FIT_MARGIN * Math.min(w, h) / playfieldSize;
    }

    /**
     * Rebuild {@link #puzzleToView} (and its inverse) from the current
     * view size and display scale.  The puzzle origin maps to the center
     * of the view; puzzle Y already increases downward, matching the
     * screen, so no axis is flipped.
     */
    private void rebuildTransform() {
        float scale = getUnitScale();
        puzzleToView.reset();
        puzzleToView.postScale(scale, scale);
        puzzleToView.postTranslate(getWidth() / 2f, getHeight() / 2f);
        puzzleToView.invert(viewToPuzzle);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(getClass().getSimpleName(), String.format(Locale.US,
                "onSizeChanged(%d×%d → %d×%d)", oldw, oldh, w, h));
        super.onSizeChanged(w, h, oldw, oldh);
        computeFitScale();
        rebuildTransform();
        // Force redrawing the whole puzzle
        invalidate();
    }

    // For debugging
    long lastDrawMessageTime = 0;
    int suppressedDrawMessages = 0;
    long lastSuppressedMessageTime = 0;

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        /*
         * Log the call to DEBUG, but avoid spamming too many calls.
         * We'll allow one message per second.
         */
        long now = System.nanoTime();
        if (now - lastDrawMessageTime < 1_000_000_000) {
            suppressedDrawMessages++;
            lastSuppressedMessageTime = now;
        } else {
            if (suppressedDrawMessages > 0)
                Log.d(getClass().getSimpleName(), String.format(Locale.US,
                        ".onDraw() (%d prior calls suppressed for %.3f seconds)",
                        suppressedDrawMessages,
                        (lastSuppressedMessageTime - lastDrawMessageTime)
                                / 1_000_000_000.0));
            else
                Log.d(getClass().getSimpleName(), ".onDraw()");
            lastDrawMessageTime = now;
            suppressedDrawMessages = 0;
        }
        super.onDraw(canvas);

        Resources.Theme currentTheme = getContext().getTheme();
        int bgColor = getResources().getColor(R.color.invisible, currentTheme);
        canvas.drawColor(bgColor);

        // Nothing to draw until we have pieces and a measured size.
        if (pieces.isEmpty() || fitScale <= 0f)
            return;

        float scale = getUnitScale();
        int w = getWidth(), h = getHeight();

        outlinePaint.setColor(outlineColor);
        outlinePaint.setStrokeWidth(outlineWidthPx);

        for (int i = 0; i < pieces.size(); i++) {
            TangramPiece piece = pieces.get(i);

            /*
             * Cheap culling: skip any piece whose bounding circle lies
             * entirely outside the visible area.  (Off-playfield pieces
             * will eventually live in a separate tray view.)
             */
            pointBuffer[0] = piece.getPosition().getX();
            pointBuffer[1] = piece.getPosition().getY();
            puzzleToView.mapPoints(pointBuffer);
            float radius = piece.getMaxRadius() * scale;
            if (pointBuffer[0] + radius < 0 || pointBuffer[0] - radius > w
                    || pointBuffer[1] + radius < 0 || pointBuffer[1] - radius > h)
                continue;

            // Build the piece outline from its transformed vertices.
            TPoint[] vertices = piece.getVertices();
            if (vertexBuffer.length < vertices.length * 2)
                vertexBuffer = new float[vertices.length * 2];
            for (int v = 0; v < vertices.length; v++) {
                vertexBuffer[v * 2] = vertices[v].getX();
                vertexBuffer[v * 2 + 1] = vertices[v].getY();
            }
            puzzleToView.mapPoints(vertexBuffer, 0, vertexBuffer, 0, vertices.length);

            piecePath.rewind();
            piecePath.moveTo(vertexBuffer[0], vertexBuffer[1]);
            for (int v = 1; v < vertices.length; v++)
                piecePath.lineTo(vertexBuffer[v * 2], vertexBuffer[v * 2 + 1]);
            piecePath.close();

            fillPaint.setColor(fillColors.get(piece.getColorAttr(), Color.GRAY));
            canvas.drawPath(piecePath, fillPaint);
            if (outlineWidthPx > 0f)
                canvas.drawPath(piecePath, outlinePaint);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                int pointerIndex = event.getActionIndex();
                mapTouchToPuzzle(
                        event.getX(pointerIndex), event.getY(pointerIndex));
                TangramPiece hit = pieceAt(touchBuffer[0], touchBuffer[1]);
                selectedPiece = hit;
                if (hit == null) {
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    invalidate();
                    // Consume the gesture anyway so a tap on the felt can
                    // deselect; later this branch can start a background pan.
                    return true;
                }
                activePointerId = event.getPointerId(pointerIndex);
                raiseToTop(hit);
                grabOffsetX = hit.getPosition().getX() - touchBuffer[0];
                grabOffsetY = hit.getPosition().getY() - touchBuffer[1];
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                if (selectedPiece == null
                        || activePointerId == MotionEvent.INVALID_POINTER_ID)
                    return false;
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex < 0)
                    return false;
                mapTouchToPuzzle(
                        event.getX(pointerIndex), event.getY(pointerIndex));
                // Free placement for now; snapping happens on release.
                selectedPiece.setPosition(new TPoint(
                        touchBuffer[0] + grabOffsetX, 0,
                        touchBuffer[1] + grabOffsetY, 0));
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                // If the dragging finger lifted, stop dragging but keep the
                // selection.  (A second held pointer is where two-finger
                // rotation will hook in.)
                int pointerIndex = event.getActionIndex();
                if (event.getPointerId(pointerIndex) == activePointerId)
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                return true;
            }

            case MotionEvent.ACTION_UP:
                performClick();
                // fall through
            case MotionEvent.ACTION_CANCEL:
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                // To Do: snap the released piece's vertices/edges to its
                // neighbors (and to the solution outline) here.
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    /**
     * Convert a touch point from view (pixel) coordinates into puzzle
     * coordinates, leaving the result in {@link #touchBuffer}.
     */
    private void mapTouchToPuzzle(float x, float y) {
        touchBuffer[0] = x;
        touchBuffer[1] = y;
        viewToPuzzle.mapPoints(touchBuffer);
    }

    /**
     * Find the topmost piece containing the given puzzle-space point.
     *
     * @return the hit piece, or {@code null} if the point is on no piece
     */
    @Nullable
    private TangramPiece pieceAt(float px, float py) {
        for (int i = pieces.size() - 1; i >= 0; i--) {
            TangramPiece piece = pieces.get(i);
            if (contains(piece.getVertices(), px, py))
                return piece;
        }
        return null;
    }

    /** Move a piece to the top of the draw / hit-test order. */
    private void raiseToTop(TangramPiece piece) {
        if (pieces.remove(piece))
            pieces.add(piece);
    }

    /**
     * Test whether a point lies within a convex polygon.  All Tangram
     * pieces are convex, so the point is inside exactly when it stays on
     * the same side of every edge (cross products all share one sign);
     * a zero cross product counts as on-edge, i.e. inside.
     *
     * @param vertices the polygon vertices in order (winding may be either
     *                 direction)
     * @param px the point&rsquo;s X coordinate, in the same space as the vertices
     * @param py the point&rsquo;s Y coordinate, in the same space as the vertices
     */
    private static boolean contains(@NonNull TPoint[] vertices, float px, float py) {
        boolean positive = false, negative = false;
        int n = vertices.length;
        for (int i = 0; i < n; i++) {
            TPoint a = vertices[i];
            TPoint b = vertices[(i + 1) % n];
            float cross = (b.getX() - a.getX()) * (py - a.getY())
                    - (b.getY() - a.getY()) * (px - a.getX());
            if (cross > 0f)
                positive = true;
            else if (cross < 0f)
                negative = true;
            if (positive && negative)
                return false;
        }
        return true;
    }

}