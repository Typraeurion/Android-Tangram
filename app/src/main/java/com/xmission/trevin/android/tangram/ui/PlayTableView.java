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
import android.view.View;

import androidx.annotation.Nullable;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.TPoint;
import com.xmission.trevin.android.tangram.data.TangramPiece;
import com.xmission.trevin.android.tangram.data.TangramPuzzle;

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
     * Reference to the pieces that the player is working with
     */
    TangramPuzzle activePuzzle;

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
     * Set the puzzle whose pieces this view displays.  Recomputes the
     * fit-to-view scale if the view has already been laid out.
     *
     * @param puzzle the puzzle to display; must not be {@code null}
     */
    public void setPuzzle(TangramPuzzle puzzle) {
        activePuzzle = puzzle;
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
        int[] attrs = {
                R.attr.tangramOutlineColor,
                R.attr.tangramOutlineWidth,
                R.attr.tangramSmallTriangleColor,
                R.attr.tangramSmallSquareColor,
                R.attr.tangramParallelogramColor,
                R.attr.tangramMediumTriangleColor,
                R.attr.tangramLargeTriangleColor,
        };
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs);
        try {
            outlineColor = a.getColor(0, Color.BLACK);
            outlineWidthPx = resolveDimension(a, 1);
            fillColors.clear();
            fillColors.put(R.attr.tangramSmallTriangleColor, a.getColor(2, Color.GRAY));
            fillColors.put(R.attr.tangramSmallSquareColor, a.getColor(3, Color.GRAY));
            fillColors.put(R.attr.tangramParallelogramColor, a.getColor(4, Color.GRAY));
            fillColors.put(R.attr.tangramMediumTriangleColor, a.getColor(5, Color.GRAY));
            fillColors.put(R.attr.tangramLargeTriangleColor, a.getColor(6, Color.GRAY));
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
    private float resolveDimension(TypedArray a, int index) {
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
        if (activePuzzle == null || w == 0 || h == 0)
            return;
        fitScale = FIT_MARGIN * Math.min(w, h) / activePuzzle.getSize();
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
    protected void onDraw(Canvas canvas) {
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

        // Nothing to draw until we have a puzzle and a measured size.
        if (activePuzzle == null || fitScale <= 0f)
            return;

        float scale = getUnitScale();
        int w = getWidth(), h = getHeight();

        outlinePaint.setColor(outlineColor);
        outlinePaint.setStrokeWidth(outlineWidthPx);

        for (int i = 0; i < activePuzzle.getPieceCount(); i++) {
            TangramPiece piece = activePuzzle.getPiece(i);

            /*
             * Cheap culling: skip any piece whose bounding circle lies
             * entirely outside the visible area.  (Off-playfield pieces
             * will eventually live in a separate tray view.)
             */
            float[] center = {
                    piece.getPosition().getX(), piece.getPosition().getY() };
            puzzleToView.mapPoints(center);
            float radius = piece.getMaxRadius() * scale;
            if (center[0] + radius < 0 || center[0] - radius > w
                    || center[1] + radius < 0 || center[1] - radius > h)
                continue;

            // Build the piece outline from its transformed vertices.
            TPoint[] vertices = piece.getVertices();
            float[] xy = new float[vertices.length * 2];
            for (int v = 0; v < vertices.length; v++) {
                xy[v * 2] = vertices[v].getX();
                xy[v * 2 + 1] = vertices[v].getY();
            }
            puzzleToView.mapPoints(xy);

            piecePath.rewind();
            piecePath.moveTo(xy[0], xy[1]);
            for (int v = 1; v < vertices.length; v++)
                piecePath.lineTo(xy[v * 2], xy[v * 2 + 1]);
            piecePath.close();

            fillPaint.setColor(fillColors.get(piece.getColorAttr(), Color.GRAY));
            canvas.drawPath(piecePath, fillPaint);
            if (outlineWidthPx > 0f)
                canvas.drawPath(piecePath, outlinePaint);
        }
    }

    // To Do: handle onTouchEvent

}