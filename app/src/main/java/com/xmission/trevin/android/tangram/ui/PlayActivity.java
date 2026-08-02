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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.IntentCompat;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.*;

import java.util.Locale;

/**
 * The screen where the player actually assembles a Tangram.  It hosts a
 * {@link PlayTableView} for the play surface and a tray of
 * {@link PieceTrayItemView}s&mdash;one per kind of piece&mdash;from which
 * pieces can be dragged onto the surface.
 *
 * <p>This is not the launcher activity; it is started from the home
 * screen either for a chosen goal puzzle (via {@link #createIntent}) or
 * for free-play / sketch mode (with a {@code null} puzzle).</p>
 */
public class PlayActivity extends TangramActivity {

    private static final String LOG_TAG = "PlayActivity";

    /**
     * Intent extra carrying the goal puzzle.  Absent for free-play mode.
     */
    public static final String EXTRA_PUZZLE_GOAL =
            "com.xmission.trevin.android.tangram.PUZZLE_GOAL";

    private PlayTableView playTableView;

    /**
     * Build an intent to start this activity.
     *
     * @param context the launching context
     * @param goal    the goal puzzle to solve, or {@code null} for free play
     * @return the intent to hand to {@code startActivity}
     */
    @NonNull
    public static Intent createIntent(
            @NonNull Context context, @Nullable TangramPuzzle goal) {
        Intent intent = new Intent(context, PlayActivity.class);
        if (goal != null)
            intent.putExtra(EXTRA_PUZZLE_GOAL, goal);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // TangramActivity applies the hint-level theme (piece colors) before
        // the content view is inflated.
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, String.format(Locale.US,
                "onCreate(%s)", savedInstanceState == null ? "" : "saved state"));
        setContentView(R.layout.activity_play);
        WindowInsetsUtil.applySafeAreaPadding(this);

        playTableView = findViewById(R.id.play_table);
        TangramPuzzle goal = IntentCompat.getParcelableExtra(
                getIntent(), EXTRA_PUZZLE_GOAL, TangramPuzzle.class);
        playTableView.setSolution(goal);
        setUpDropTarget();
        setUpOverlayControls(goal);
        configureTraySlots();
    }

    /**
     * Give each tray slot the play field&rsquo;s scale so a dragged piece
     * previews at the size it will have once dropped.
     */
    private void configureTraySlots() {
        ViewGroup tray = findViewById(R.id.piece_tray);
        for (int i = 0; i < tray.getChildCount(); i++) {
            View child = tray.getChildAt(i);
            if (child instanceof PieceTrayItemView)
                ((PieceTrayItemView) child).setUnitScaleProvider(
                        playTableView::getUnitScale);
        }
    }

    /**
     * Wire up the controls that hover over the play area: the back / exit
     * button, the goal preview (shown only when solving a puzzle), and the
     * contextual flip button (shown only while a flippable piece&mdash;the
     * parallelogram&mdash;is selected).
     *
     * @param goal the goal puzzle to display if any, or {@code null}
     * for free-play mode
     */
    private void setUpOverlayControls(@Nullable TangramPuzzle goal) {
        findViewById(R.id.button_back).setOnClickListener(v -> finish());

        // To Do: render the goal as a target silhouette; for now this is
        // just a placeholder panel that only appears in puzzle mode.
        TangramPuzzleView goalView = findViewById(R.id.goal_view);
        if (goal != null) {
            goalView.setPuzzle(goal);
            goalView.setVisibility(View.VISIBLE);
        } else {
            goalView.setVisibility(View.GONE);
        }

        ImageButton flipButton = findViewById(R.id.button_flip);
        flipButton.setOnClickListener(v -> playTableView.flipSelectedPiece());
        playTableView.setOnSelectionChangedListener(selected ->
                flipButton.setVisibility(canFlip(selected)
                        ? View.VISIBLE : View.GONE));
        // Reflect the current selection (normally none on a fresh start).
        flipButton.setVisibility(
                canFlip(playTableView.getSelectedPiece())
                        ? View.VISIBLE : View.GONE);
    }

    /** @return whether a (possibly {@code null}) piece can be flipped. */
    private static boolean canFlip(@Nullable TangramPiece piece) {
        return piece != null && piece.canFlip();
    }

    /**
     * Make the play surface accept pieces dropped from the tray, creating
     * a fresh piece there and decrementing that kind&rsquo;s tray count.
     */
    private void setUpDropTarget() {
        playTableView.setOnDragListener((view, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                case DragEvent.ACTION_DRAG_ENTERED:
                case DragEvent.ACTION_DRAG_LOCATION:
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    return true;

                case DragEvent.ACTION_DROP:
                    Object state = event.getLocalState();
                    if (state instanceof PieceTrayItemView) {
                        PieceTrayItemView slot = (PieceTrayItemView) state;
                        if (slot.getCount() > 0) {
                            TangramPiece piece = slot.createPiece();
                            playTableView.addPieceAtViewLocation(
                                    piece, event.getX(), event.getY());
                            slot.decrement();
                        }
                    }
                    return true;

                default:
                    return false;
            }
        });

        // A piece dragged off the field comes back to its tray slot.
        playTableView.setOnPieceReturnedListener(this::returnPieceToTray);
    }

    /**
     * Return a piece that was dragged off the play field to its tray slot,
     * bumping that slot&rsquo;s available count back up.
     *
     * @param piece the piece that left the field
     */
    private void returnPieceToTray(@NonNull TangramPiece piece) {
        ViewGroup tray = findViewById(R.id.piece_tray);
        for (int i = 0; i < tray.getChildCount(); i++) {
            View child = tray.getChildAt(i);
            if (child instanceof PieceTrayItemView) {
                PieceTrayItemView slot = (PieceTrayItemView) child;
                if (slot.accepts(piece)) {
                    slot.setCount(slot.getCount() + 1);
                    return;
                }
            }
        }
    }
}