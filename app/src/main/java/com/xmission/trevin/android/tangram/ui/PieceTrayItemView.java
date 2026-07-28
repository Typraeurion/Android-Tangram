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

import android.content.ClipData;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.*;

/**
 * A single slot in the piece tray: it shows an image of one type of
 * {@link TangramPiece} (in the current theme colors) with a badge for
 * how many of that type are still available to add to the play field,
 * and it starts a drag-and-drop gesture so the player can drag a new
 * piece onto the {@link PlayTableView}.
 *
 * <p>The kind of piece and its starting count are declared in the layout
 * via the {@code app:pieceType} and {@code app:pieceCount} attributes.
 * Each slot holds a {@link PieceFactory} rather than a single piece so
 * that every drag creates a fresh, independent piece.</p>
 */
public class PieceTrayItemView extends FrameLayout {

    /** Creates new instances of one kind of piece for this slot. */
    public interface PieceFactory {
        @NonNull TangramPiece create();
    }

    /**
     * The kinds of piece a tray slot can dispense.  The order MUST match
     * the {@code pieceType} enum in {@code attrs.xml}, since the attribute
     * value indexes into {@link #values()}.
     */
    public enum PieceType {
        SMALL_TRIANGLE(TangramSmallTriangle::new, R.string.piece_small_triangle),
        SQUARE(TangramSquare::new, R.string.piece_square),
        PARALLELOGRAM(TangramParallelogram::new, R.string.piece_parallelogram),
        MEDIUM_TRIANGLE(TangramMediumTriangle::new, R.string.piece_medium_triangle),
        LARGE_TRIANGLE(TangramLargeTriangle::new, R.string.piece_large_triangle);

        final PieceFactory factory;
        @StringRes final int nameRes;

        PieceType(PieceFactory factory, @StringRes int nameRes) {
            this.factory = factory;
            this.nameRes = nameRes;
        }
    }

    private ImageView pieceImage;
    private TextView countText;

    /** Factory for the kind of piece this slot dispenses. */
    @Nullable
    private PieceFactory factory;

    /** Human-readable name of the piece, for content descriptions. */
    @Nullable
    private CharSequence pieceName;

    /** How many pieces of this kind remain available to add. */
    private int count;

    public PieceTrayItemView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public PieceTrayItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.PieceTrayItemView);
        try {
            int typeIndex = a.getInt(R.styleable.PieceTrayItemView_pieceType, 0);
            int initialCount = a.getInt(R.styleable.PieceTrayItemView_pieceCount, 0);
            PieceType[] types = PieceType.values();
            if (typeIndex < 0 || typeIndex >= types.length)
                typeIndex = 0;
            setPieceType(types[typeIndex], initialCount);
        } finally {
            a.recycle();
        }
    }

    private void init(@NonNull Context context) {
        LayoutInflater.from(context).inflate(
                R.layout.view_piece_tray_item, this, true);
        pieceImage = findViewById(R.id.piece_image);
        countText = findViewById(R.id.piece_count);
        // The whole slot is the accessibility target and click target.
        setFocusable(true);
        setClickable(true);
    }

    /**
     * Configure this slot for a kind of piece and its starting count.
     *
     * @param type  the kind of piece this slot dispenses
     * @param count the initial number available to add
     */
    public void setPieceType(@NonNull PieceType type, int count) {
        this.factory = type.factory;
        this.pieceName = getResources().getText(type.nameRes);
        // A sample instance tells us which drawable to display.  The
        // drawable references the theme's tangram*Color attributes, so
        // AppCompatResources renders it in the current mode's colors.
        TangramPiece sample = type.factory.create();
        pieceImage.setImageDrawable(AppCompatResources.getDrawable(
                getContext(), sample.getDrawableId()));
        setCount(count);
    }

    /** @return how many pieces of this kind remain available to add. */
    public int getCount() {
        return count;
    }

    /** Set the available count, updating the badge and enabled state. */
    public void setCount(int count) {
        this.count = Math.max(0, count);
        countText.setText(getResources().getString(
                R.string.piece_count_format, this.count));
        // Dim and disable the slot when the player has used all of them.
        boolean available = this.count > 0;
        setEnabled(available);
        setAlpha(available ? 1f : 0.35f);
        setContentDescription(getResources().getString(
                R.string.piece_tray_item_description,
                pieceName == null ? "" : pieceName, this.count));
    }

    /** Reduce the available count by one (never below zero). */
    public void decrement() {
        setCount(count - 1);
    }

    /** Create a fresh piece of this slot&rsquo;s kind. */
    @NonNull
    public TangramPiece createPiece() {
        if (factory == null)
            throw new IllegalStateException("Tray slot was not configured");
        return factory.create();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        // Touching a stocked slot starts dragging a new piece out of it.
        // The actual piece is created and placed when it is dropped on the
        // PlayTableView (see PlayActivity's OnDragListener).
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                && count > 0 && factory != null) {
            startPieceDrag();
            return true;
        }
        return super.onTouchEvent(event);
    }

    /** Begin a drag-and-drop gesture carrying this slot as its state. */
    @SuppressWarnings("deprecation") // startDrag for minSdk < 24 (N)
    private void startPieceDrag() {
        ClipData clip = ClipData.newPlainText(
                pieceName == null ? "" : pieceName, "");
        View.DragShadowBuilder shadow = new View.DragShadowBuilder(pieceImage);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            startDragAndDrop(clip, shadow, this, 0);
        else
            startDrag(clip, shadow, this, 0);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    /*
     * The inner image and count label are the same layout inflated into
     * every slot, so they share view ids.  Save/restore only this slot's
     * own state (its count) to keep those duplicate ids from colliding in
     * the id-keyed hierarchy state.
     */

    @Override
    protected void dispatchSaveInstanceState(@NonNull SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(@NonNull SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.count = count;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setCount(ss.count);
    }

    /** Saved state for a tray slot: how many pieces remain available. */
    private static class SavedState extends BaseSavedState {

        int count;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            count = in.readInt();
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(count);
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