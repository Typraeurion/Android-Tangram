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
import android.view.View;

import androidx.annotation.NonNull;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.TPoint;
import com.xmission.trevin.android.tangram.data.TangramPiece;
import com.xmission.trevin.android.tangram.data.TangramPuzzle;

import java.util.Locale;

/**
 * A static view of a Tangram puzzle showing its final shape.
 * This is used for both showing the library of available puzzles
 * and showing the goal of a game in progress.
 *
 * @author Trevin Beattie, based on {@link PlayTableView} code by
 * Claude Opus 4.8
 */
public class TangramPuzzleView extends View {

    private static final String LOG_TAG = "TangramPuzzleView";

    /** Fraction of the view kept as margin around the puzzle when fitting. */
    private static final float FIT_MARGIN = 0.95f;

    /** Reference to the puzzle being shown */
    private TangramPuzzle puzzle;

    /** Paint used to fill the interior of each piece. */
    private final Paint fillPaint = new Paint();

    /** Paint used to stroke the outline of each piece. */
    private final Paint outlinePaint = new Paint();

    /** Reusable path for building a piece&rsquo;s outline in {@link #onDraw}. */
    private final Path piecePath = new Path();

    /**
     * Reusable buffer for mapping a piece&rsquo;s vertices, as interleaved
     * (x, y) pairs.  Grown on demand for pieces with more vertices.
     */
    private float[] vertexBuffer = new float[8];

    /** Maps puzzle coordinates to view (pixel) coordinates. */
    private final Matrix puzzleToView = new Matrix();

    /**
     * Display scale, in pixels per puzzle unit, at which the whole
     * puzzle area just fits within the view.  Recomputed whenever the
     * view is resized or the puzzle changes.  Zero means &ldquo;not yet
     * measured&rdquo;, in which case nothing is drawn.
     */
    private float fitScale = 0f;

    /** Cached outline color from the current theme. */
    private int outlineColor = Color.BLACK;

    /** Cached outline width, in pixels, from the current theme. */
    private float outlineWidthPx = 0f;

    /** Cached fill colors from the current theme, keyed by color attribute. */
    private final SparseIntArray fillColors = new SparseIntArray();

    public TangramPuzzleView(Context context) {
        super(context);
        init();
    }

    public TangramPuzzleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /** Initialize the view for the configured theme */
    private void init() {
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);
        outlinePaint.setAntiAlias(true);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeJoin(Paint.Join.MITER);
        refreshThemeCache();
    }

    /**
     * Set the puzzle to show.
     *
     * @param puzzle the puzzle to show; must not be {@code null}
     */
    public void setPuzzle(@NonNull TangramPuzzle puzzle) {
        this.puzzle = puzzle;
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

    /**
     * Compute {@link #fitScale} so the puzzle&rsquo;s full extent fits
     * within the current view bounds (less {@link #FIT_MARGIN}).
     */
    private void computeFitScale() {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0)
            return;
        fitScale = FIT_MARGIN * Math.min(w, h) / puzzle.getSize();
    }

    /**
     * Rebuild {@link #puzzleToView} from the current view size and
     * display scale.  The puzzle origin maps to the center of the
     * view; puzzle Y already increases downward, matching the
     * screen, so no axis is flipped.
     */
    private void rebuildTransform() {
        puzzleToView.reset();
        puzzleToView.postScale(fitScale, fitScale);
        puzzleToView.postTranslate(getWidth() / 2f, getHeight() / 2f);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Skip logging if this is the first time the view has been measured
        if (oldw * oldh != 0)
            Log.d(LOG_TAG, String.format(Locale.US,
                    "onSizeChanged(%d×%d → %d×%d)", oldw, oldh, w, h));
        super.onSizeChanged(w, h, oldw, oldh);
        computeFitScale();
        rebuildTransform();
        // Force redrawing the whole puzzle
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        Resources.Theme currentTheme = getContext().getTheme();
        int bgColor = getResources().getColor(R.color.invisible, currentTheme);
        canvas.drawColor(bgColor);

        // Nothing to draw until we have a puzzle and a measured size.
        if ((puzzle == null) || (fitScale <= 0f))
            return;

        outlinePaint.setColor(outlineColor);
        outlinePaint.setStrokeWidth(outlineWidthPx);

        for (int i = 0; i < puzzle.getPieceCount(); i++) {
            TangramPiece piece = puzzle.getPiece(i);

            // Build the piece outline from its transformed vertices.
            TPoint[] vertices = piece.getVertices();
            if (vertexBuffer.length < vertices.length * 2)
                vertexBuffer = new float[vertices.length * 2];
            for (int v = 0; v < vertices.length; v++) {
                vertexBuffer[v * 2] = (float) vertices[v].getX();
                vertexBuffer[v * 2 + 1] = (float) vertices[v].getY();
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
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.puzzle = puzzle;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState ss)) {
            super.onRestoreInstanceState(state);
            return;
        }
        super.onRestoreInstanceState(ss.getSuperState());
        // fitScale is recomputed on the next onSizeChanged; rebuild now so
        // the restored zoom is applied if we already have a size.
        rebuildTransform();
        invalidate();
    }

    /**
     * Saved state for a {@link TangramPuzzleView}.
     */
    private static class SavedState extends BaseSavedState {
        TangramPuzzle puzzle;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            puzzle = in.readTypedObject(TangramPuzzle.CREATOR);
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeTypedObject(puzzle, flags);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<>() {
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
