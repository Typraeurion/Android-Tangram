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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.TPoint;
import com.xmission.trevin.android.tangram.data.TangramPiece;
import com.xmission.trevin.android.tangram.data.TangramPuzzle;

import java.util.ArrayList;
import java.util.Collections;
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

    private static final String LOG_TAG = "PlayTableView";

    /**
     * Reference to the current puzzle the player is trying to solve,
     * or {@code null} if the player is working in freestyle / sketch mode.
     */
    @Nullable
    private TangramPuzzle solution;

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
     * Default playfield extent, in puzzle units, used for free-play /
     * sketch mode when no goal puzzle sets a size of its own.  This is
     * the size of nine (3&times;3) compact squares, matching
     * {@link TangramPuzzle}&rsquo;s default.
     */
    private static final float DEFAULT_PLAYFIELD_SIZE = 36f;

    /**
     * The playfield extent in puzzle units, used to fit the board to the
     * view.  Defaults to {@link #DEFAULT_PLAYFIELD_SIZE} until
     * {@link #setPuzzle} or {@link #setSolution} overrides it.
     */
    private float playfieldSize = DEFAULT_PLAYFIELD_SIZE;

    /** Reusable buffer for mapping a touch point into puzzle space. */
    private final float[] touchBuffer = new float[2];

    /** The piece currently grabbed by the player, or {@code null}. */
    @Nullable
    private TangramPiece selectedPiece;

    /**
     * Notified whenever {@link #selectedPiece} changes, so a host (e.g. the
     * activity) can update contextual controls such as the flip button.
     */
    @Nullable
    private OnSelectionChangedListener selectionListener;

    /**
     * Callback for a change in which piece is selected.
     */
    public interface OnSelectionChangedListener {
        /**
         * @param selected the newly selected piece, or {@code null} if
         * nothing is selected
         */
        void onSelectionChanged(@Nullable TangramPiece selected);
    }

    /**
     * Notified when a piece is dragged off the play field, so a host can
     * return it to the tray instead of letting it be lost.
     */
    @Nullable
    private OnPieceReturnedListener pieceReturnedListener;

    /**
     * Callback for a piece that has left the play field.
     */
    public interface OnPieceReturnedListener {
        /**
         * @param piece the piece that was removed from the field
         */
        void onPieceReturnedToTray(@NonNull TangramPiece piece);
    }

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

    /**
     * The id of the second pointer that, together with
     * {@link #activePointerId}, drives a two-finger rotation of
     * {@link #selectedPiece}, or {@link MotionEvent#INVALID_POINTER_ID}
     * when no rotation gesture is in progress.  While a rotation is in
     * progress the piece spins about its centroid and translation (drag)
     * is suspended.
     */
    private int rotationPointerId = MotionEvent.INVALID_POINTER_ID;

    /**
     * Angle, in degrees, of the vector from the {@link #activePointerId}
     * pointer to the {@link #rotationPointerId} pointer as of the most
     * recent motion event.  The change in this angle between events is
     * applied to {@link #selectedPiece} via
     * {@link TangramPiece#fineRotateDegrees(float)}.
     */
    private float lastRotationAngle;

    /**
     * Pan offset, in pixels, of the puzzle origin from the view center.
     * Applied by {@link #rebuildTransform()} and adjusted by pinch-zoom and
     * one-finger scrolling; kept within {@link #clampPan()}.
     */
    private float panX = 0f, panY = 0f;

    /**
     * True while a two-finger field gesture (pinch-zoom, which also pans by
     * the focal point) is in progress.  Mutually exclusive with rotating a
     * piece: a field gesture only starts when no piece is being dragged.
     */
    private boolean fieldGesturing = false;

    /** The two pointer ids driving the current field (pinch) gesture. */
    private int pinchPointerId0 = MotionEvent.INVALID_POINTER_ID;
    private int pinchPointerId1 = MotionEvent.INVALID_POINTER_ID;

    /** Distance between the pinch pointers as of the last motion event. */
    private float lastPinchDistance;

    /** Focal point (midpoint) of the pinch pointers at the last event. */
    private float lastFocalX, lastFocalY;

    /**
     * The id of the pointer scrolling (panning) the field with one finger,
     * or {@link MotionEvent#INVALID_POINTER_ID} when not scrolling.  Only
     * used when the field is zoomed in far enough to exceed the viewport.
     */
    private int panPointerId = MotionEvent.INVALID_POINTER_ID;

    /** Last one-finger scroll position, in view pixels. */
    private float lastPanTouchX, lastPanTouchY;

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
    public void setPuzzle(@NonNull TangramPuzzle puzzle) {
        playfieldSize = puzzle.getSize();
        pieces.clear();
        for (int i = 0; i < puzzle.getPieceCount(); i++)
            pieces.add(puzzle.getPiece(i));
        setSelectedPiece(null);
        activePointerId = MotionEvent.INVALID_POINTER_ID;
        panX = panY = 0f;
        computeFitScale();
        rebuildTransform();
        invalidate();
    }

    /**
     * Set the goal puzzle the player is trying to reproduce (or
     * {@code null} for free-play / sketch mode) and adopt its playfield
     * size, <em>without</em> placing any of its pieces: in a goal puzzle
     * the pieces start in the tray and the player drags them in.  The
     * goal is retained so it can later be drawn as a target silhouette
     * and used to check the solution.
     *
     * @param puzzle the goal puzzle, or {@code null} for free play
     */
    public void setSolution(@Nullable TangramPuzzle puzzle) {
        solution = puzzle;
        playfieldSize = (puzzle != null)
                ? puzzle.getSize() : DEFAULT_PLAYFIELD_SIZE;
        pieces.clear();
        setSelectedPiece(null);
        activePointerId = MotionEvent.INVALID_POINTER_ID;
        rotationPointerId = MotionEvent.INVALID_POINTER_ID;
        panX = panY = 0f;
        computeFitScale();
        rebuildTransform();
        invalidate();
    }

    /** @return the current goal puzzle, or {@code null} in free play. */
    @Nullable
    public TangramPuzzle getSolution() {
        return solution;
    }

    /**
     * Add a piece to the play field at a point given in this view&rsquo;s
     * pixel coordinates, e.g. where the player dropped it from the tray.
     * The piece&rsquo;s centroid is placed at that point, it is put on top
     * of the z-order, and it becomes the current selection.
     *
     * @param piece the piece to add
     * @param viewX the drop X coordinate, in view (pixel) space
     * @param viewY the drop Y coordinate, in view (pixel) space
     */
    public void addPieceAtViewLocation(
            @NonNull TangramPiece piece, float viewX, float viewY) {
        mapTouchToPuzzle(viewX, viewY);
        piece.setPosition(new TPoint(touchBuffer[0], 0, touchBuffer[1], 0));
        pieces.add(piece);
        setSelectedPiece(piece);
        activePointerId = MotionEvent.INVALID_POINTER_ID;
        rotationPointerId = MotionEvent.INVALID_POINTER_ID;
        invalidate();
    }

    /**
     * Register a listener to hear when the selected piece changes.
     *
     * @param listener the listener, or {@code null} to clear it
     */
    public void setOnSelectionChangedListener(
            @Nullable OnSelectionChangedListener listener) {
        selectionListener = listener;
    }

    /** @return the currently selected piece, or {@code null}. */
    @Nullable
    public TangramPiece getSelectedPiece() {
        return selectedPiece;
    }

    /**
     * Register a listener to hear when a piece is dragged off the field.
     *
     * @param listener the listener, or {@code null} to clear it
     */
    public void setOnPieceReturnedListener(
            @Nullable OnPieceReturnedListener listener) {
        pieceReturnedListener = listener;
    }

    /**
     * If the selected piece&rsquo;s centroid has been dragged outside the
     * visible play area, remove it from the field and hand it back to the
     * tray (via {@link #pieceReturnedListener}) so it is not lost.  Called
     * when a drag ends.
     */
    private void returnSelectedPieceIfOffField() {
        if (selectedPiece == null)
            return;
        pointBuffer[0] = selectedPiece.getPosition().getX();
        pointBuffer[1] = selectedPiece.getPosition().getY();
        puzzleToView.mapPoints(pointBuffer);
        float x = pointBuffer[0], y = pointBuffer[1];
        if (x >= 0 && x <= getWidth() && y >= 0 && y <= getHeight())
            return; // centroid still on the field; keep the piece
        TangramPiece removed = selectedPiece;
        pieces.remove(removed);
        setSelectedPiece(null);
        invalidate();
        if (pieceReturnedListener != null)
            pieceReturnedListener.onPieceReturnedToTray(removed);
    }

    /**
     * Change the selection, notifying {@link #selectionListener} only when
     * the selected piece actually changes.
     */
    private void setSelectedPiece(@Nullable TangramPiece piece) {
        if (selectedPiece != piece) {
            selectedPiece = piece;
            if (selectionListener != null)
                selectionListener.onSelectionChanged(piece);
        }
    }

    /**
     * Flip the selected piece over, if one is selected and it can be
     * flipped (only the parallelogram can); otherwise does nothing.
     */
    public void flipSelectedPiece() {
        if (selectedPiece != null && selectedPiece.canFlip()) {
            selectedPiece.flip();
            invalidate();
        }
    }

    /**
     * Re-read the piece fill colors, outline color, and outline width
     * from the current theme.  Call this after switching play modes
     * (which swaps in a different theme) so the next {@link #onDraw}
     * picks up the new styling.
     */
    public void refreshThemeCache() {

        try (TypedArray a = getContext().getTheme().obtainStyledAttributes(
                        R.styleable.PlayTableView)) {
            outlineColor = a.getColor(
                    R.styleable.PlayTableView_tangramOutlineColor, Color.BLACK);
            outlineWidthPx = a.getDimension(
                    R.styleable.PlayTableView_tableOutlineWidth, 0f);
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
        }
        invalidate();
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
            clampPan();
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
     * view size, display scale, and {@linkplain #panX pan offset}.  The
     * puzzle origin maps to the center of the view (plus the pan); puzzle Y
     * already increases downward, matching the screen, so no axis is
     * flipped.
     */
    private void rebuildTransform() {
        float scale = getUnitScale();
        puzzleToView.reset();
        puzzleToView.postScale(scale, scale);
        puzzleToView.postTranslate(
                getWidth() / 2f + panX, getHeight() / 2f + panY);
        puzzleToView.invert(viewToPuzzle);
    }

    /**
     * @return the largest pan magnitude, in pixels, along the given view
     * extent that still keeps the playfield covering the viewport; 0 when
     * the field is not larger than the viewport (so it stays centered).
     */
    private float maxPan(int viewExtent) {
        float fieldExtent = playfieldSize * getUnitScale();
        return Math.max(0f, (fieldExtent - viewExtent) / 2f);
    }

    /** @return whether the field is large enough to be scrolled. */
    private boolean canPan() {
        return maxPan(getWidth()) > 0f || maxPan(getHeight()) > 0f;
    }

    /**
     * Constrain {@link #panX}/{@link #panY} so the field cannot be scrolled
     * past its edges (and stays centered on any axis where it fits).
     */
    private void clampPan() {
        float maxX = maxPan(getWidth());
        float maxY = maxPan(getHeight());
        panX = Math.max(-maxX, Math.min(maxX, panX));
        panY = Math.max(-maxY, Math.min(maxY, panY));
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(LOG_TAG, String.format(Locale.US,
                "onSizeChanged(%d×%d → %d×%d)", oldw, oldh, w, h));
        super.onSizeChanged(w, h, oldw, oldh);
        computeFitScale();
        clampPan();
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
        if (pieces.isEmpty() || (fitScale <= 0f))
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
                setSelectedPiece(hit);
                if (hit == null) {
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    // On empty felt: start scrolling the field if it is
                    // zoomed in far enough to be pannable; otherwise this
                    // just deselects (a second finger can still start a
                    // pinch-zoom).
                    if (canPan()) {
                        panPointerId = event.getPointerId(pointerIndex);
                        lastPanTouchX = event.getX(pointerIndex);
                        lastPanTouchY = event.getY(pointerIndex);
                    } else {
                        panPointerId = MotionEvent.INVALID_POINTER_ID;
                    }
                    invalidate();
                    return true;
                }
                activePointerId = event.getPointerId(pointerIndex);
                panPointerId = MotionEvent.INVALID_POINTER_ID;
                raiseToTop(hit);
                grabOffsetX = hit.getPosition().getX() - touchBuffer[0];
                grabOffsetY = hit.getPosition().getY() - touchBuffer[1];
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                int pointerIndex = event.getActionIndex();
                int newId = event.getPointerId(pointerIndex);
                if (selectedPiece != null
                        && activePointerId != MotionEvent.INVALID_POINTER_ID
                        && rotationPointerId == MotionEvent.INVALID_POINTER_ID
                        && newId != activePointerId) {
                    // A piece is being dragged: promote to a two-finger
                    // rotation about that piece's centroid.
                    rotationPointerId = newId;
                    lastRotationAngle = angleBetweenPointers(
                            event, activePointerId, rotationPointerId);
                } else if (!fieldGesturing
                        && activePointerId == MotionEvent.INVALID_POINTER_ID
                        && rotationPointerId == MotionEvent.INVALID_POINTER_ID
                        && event.getPointerCount() >= 2) {
                    // No piece is being dragged or rotated: two fingers
                    // pinch-zoom (and pan by the focal point) the field.
                    beginFieldGesture(event);
                }
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                if (fieldGesturing) {
                    updateFieldGesture(event);
                    return true;
                }
                if (rotationPointerId != MotionEvent.INVALID_POINTER_ID
                        && selectedPiece != null) {
                    // Two-finger rotation: spin the piece about its centroid
                    // by however much the finger-to-finger angle changed.
                    // Translation is suspended for the duration.
                    float current = angleBetweenPointers(
                            event, activePointerId, rotationPointerId);
                    if (!Float.isNaN(current)) {
                        float delta = current - lastRotationAngle;
                        while (delta > 180f)
                            delta -= 360f;
                        while (delta <= -180f)
                            delta += 360f;
                        selectedPiece.fineRotateDegrees(delta);
                        lastRotationAngle = current;
                        invalidate();
                    }
                    return true;
                }
                if (panPointerId != MotionEvent.INVALID_POINTER_ID) {
                    // One-finger scroll of the field.
                    int panIndex = event.findPointerIndex(panPointerId);
                    if (panIndex < 0)
                        return false;
                    float x = event.getX(panIndex), y = event.getY(panIndex);
                    panX += x - lastPanTouchX;
                    panY += y - lastPanTouchY;
                    lastPanTouchX = x;
                    lastPanTouchY = y;
                    clampPan();
                    rebuildTransform();
                    invalidate();
                    return true;
                }
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
                int pointerIndex = event.getActionIndex();
                int liftedId = event.getPointerId(pointerIndex);
                if (fieldGesturing) {
                    // Dropping from a two-finger field gesture back to one
                    // finger: end the pinch and hand the remaining finger to
                    // a one-finger scroll (if the field is still pannable).
                    if (liftedId == pinchPointerId0 || liftedId == pinchPointerId1) {
                        int remainingId = (liftedId == pinchPointerId0)
                                ? pinchPointerId1 : pinchPointerId0;
                        endFieldGesture();
                        int remainingIndex = event.findPointerIndex(remainingId);
                        if (remainingIndex >= 0 && canPan()) {
                            panPointerId = remainingId;
                            lastPanTouchX = event.getX(remainingIndex);
                            lastPanTouchY = event.getY(remainingIndex);
                        }
                    }
                } else if (rotationPointerId != MotionEvent.INVALID_POINTER_ID) {
                    // Dropping from a two-finger rotation back to one finger.
                    // Do NOT snap the angle yet (that is deferred until a
                    // piece can attach to a neighbor); simply resume dragging
                    // with whichever finger is still down.
                    if (liftedId == rotationPointerId)
                        resumeDragWith(event, activePointerId);
                    else if (liftedId == activePointerId)
                        resumeDragWith(event, rotationPointerId);
                    // else a third finger lifted; keep rotating on the pair.
                } else if (liftedId == activePointerId) {
                    // The dragging finger lifted; keep the selection but stop
                    // dragging.
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                } else if (liftedId == panPointerId) {
                    panPointerId = MotionEvent.INVALID_POINTER_ID;
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
                performClick();
                // fall through
            case MotionEvent.ACTION_CANCEL:
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                rotationPointerId = MotionEvent.INVALID_POINTER_ID;
                panPointerId = MotionEvent.INVALID_POINTER_ID;
                endFieldGesture();
                // A piece dragged off the visible field goes back to the
                // tray rather than being lost off-screen.
                returnSelectedPieceIfOffField();
                // To Do: snap the released piece's vertices/edges to its
                // neighbors (and to the solution outline) here, and only
                // then snap its rotation to the nearest 45° via
                // TangramPiece.coarseRotate.
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Begin a two-finger field gesture (pinch-zoom, which also scrolls the
     * field by its focal point) using the first two pointers.
     */
    private void beginFieldGesture(@NonNull MotionEvent event) {
        fieldGesturing = true;
        panPointerId = MotionEvent.INVALID_POINTER_ID; // superseded
        pinchPointerId0 = event.getPointerId(0);
        pinchPointerId1 = event.getPointerId(1);
        lastPinchDistance = distanceBetweenPointers(
                event, pinchPointerId0, pinchPointerId1);
        lastFocalX = (event.getX(0) + event.getX(1)) / 2f;
        lastFocalY = (event.getY(0) + event.getY(1)) / 2f;
    }

    /** Clear all two-finger field-gesture state. */
    private void endFieldGesture() {
        fieldGesturing = false;
        pinchPointerId0 = MotionEvent.INVALID_POINTER_ID;
        pinchPointerId1 = MotionEvent.INVALID_POINTER_ID;
    }

    /**
     * Apply one motion event of the pinch gesture: rescale about the fingers&rsquo;
     * focal point and scroll so the point that was under the previous focal
     * stays under the current one.
     */
    private void updateFieldGesture(@NonNull MotionEvent event) {
        int index0 = event.findPointerIndex(pinchPointerId0);
        int index1 = event.findPointerIndex(pinchPointerId1);
        if (index0 < 0 || index1 < 0)
            return;
        float curDistance = distanceBetweenPointers(
                event, pinchPointerId0, pinchPointerId1);
        float curFocalX = (event.getX(index0) + event.getX(index1)) / 2f;
        float curFocalY = (event.getY(index0) + event.getY(index1)) / 2f;

        // Puzzle point currently under the previous focal point.
        mapTouchToPuzzle(lastFocalX, lastFocalY);
        float puzzleX = touchBuffer[0], puzzleY = touchBuffer[1];

        if (lastPinchDistance > 0f && curDistance > 0f)
            userZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM,
                    userZoom * (curDistance / lastPinchDistance)));

        // Re-anchor: place that puzzle point under the current focal point.
        float scale = getUnitScale();
        panX = curFocalX - getWidth() / 2f - puzzleX * scale;
        panY = curFocalY - getHeight() / 2f - puzzleY * scale;
        clampPan();
        rebuildTransform();
        invalidate();

        lastPinchDistance = curDistance;
        lastFocalX = curFocalX;
        lastFocalY = curFocalY;
    }

    /**
     * @return the distance in view pixels between two pointers, or 0 if
     * either is not present in this event
     */
    private float distanceBetweenPointers(
            @NonNull MotionEvent event, int id0, int id1) {
        int index0 = event.findPointerIndex(id0);
        int index1 = event.findPointerIndex(id1);
        if (index0 < 0 || index1 < 0)
            return 0f;
        float dx = event.getX(index1) - event.getX(index0);
        float dy = event.getY(index1) - event.getY(index0);
        return (float) Math.hypot(dx, dy);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    /**
     * Compute the angle, in degrees, of the vector from the pointer with
     * id {@code fromId} to the pointer with id {@code toId} in view (pixel)
     * space.  Because {@link #puzzleToView} is a uniform scale plus a
     * translation with no reflection, this screen-space angle changes by
     * the same amount as the puzzle-space angle, and a clockwise turn on
     * screen is a clockwise turn of the piece.
     *
     * @return the angle in degrees, or {@link Float#NaN} if either pointer
     * is not present in this event
     */
    private float angleBetweenPointers(
            @NonNull MotionEvent event, int fromId, int toId) {
        int fromIndex = event.findPointerIndex(fromId);
        int toIndex = event.findPointerIndex(toId);
        if (fromIndex < 0 || toIndex < 0)
            return Float.NaN;
        float dx = event.getX(toIndex) - event.getX(fromIndex);
        float dy = event.getY(toIndex) - event.getY(fromIndex);
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    /**
     * End any two-finger rotation and resume single-finger dragging of
     * {@link #selectedPiece} with the given pointer, recomputing the grab
     * offset from that pointer&rsquo;s current position so the piece does
     * not jump.
     *
     * @param event the current motion event (the pointer must still be down)
     * @param pointerId the id of the pointer to keep dragging with
     */
    private void resumeDragWith(@NonNull MotionEvent event, int pointerId) {
        rotationPointerId = MotionEvent.INVALID_POINTER_ID;
        int pointerIndex = event.findPointerIndex(pointerId);
        if (pointerIndex < 0 || selectedPiece == null) {
            activePointerId = MotionEvent.INVALID_POINTER_ID;
            return;
        }
        activePointerId = pointerId;
        mapTouchToPuzzle(event.getX(pointerIndex), event.getY(pointerIndex));
        grabOffsetX = selectedPiece.getPosition().getX() - touchBuffer[0];
        grabOffsetY = selectedPiece.getPosition().getY() - touchBuffer[1];
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

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.pieces = pieces.toArray(new TangramPiece[0]);
        state.userZoom = userZoom;
        state.panX = panX;
        state.panY = panY;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState ss)) {
            super.onRestoreInstanceState(state);
            return;
        }
        super.onRestoreInstanceState(ss.getSuperState());
        pieces.clear();
        if (ss.pieces != null)
            Collections.addAll(pieces, ss.pieces);
        userZoom = ss.userZoom;
        panX = ss.panX;
        panY = ss.panY;
        // Any in-flight gesture is meaningless after a restore.
        setSelectedPiece(null);
        activePointerId = MotionEvent.INVALID_POINTER_ID;
        rotationPointerId = MotionEvent.INVALID_POINTER_ID;
        panPointerId = MotionEvent.INVALID_POINTER_ID;
        endFieldGesture();
        // fitScale is recomputed on the next onSizeChanged; rebuild now so
        // the restored zoom is applied if we already have a size.
        clampPan();
        rebuildTransform();
        invalidate();
    }

    /**
     * Saved state for a {@link PlayTableView}: the placed pieces (in
     * back-to-front draw / hit-test order) and the user zoom multiplier.
     * The goal puzzle is <em>not</em> saved here&mdash;it is restored from
     * the launching intent&mdash;so the playfield size follows from it.
     */
    private static class SavedState extends BaseSavedState {

        TangramPiece[] pieces;
        float userZoom = 1f;
        float panX = 0f, panY = 0f;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            pieces = in.createTypedArray(TangramPiece.CREATOR);
            userZoom = in.readFloat();
            panX = in.readFloat();
            panY = in.readFloat();
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeTypedArray(pieces, flags);
            out.writeFloat(userZoom);
            out.writeFloat(panX);
            out.writeFloat(panY);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

}
