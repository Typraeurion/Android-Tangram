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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
public class PlayActivity extends AppCompatActivity {

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
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, String.format(Locale.US,
                "onCreate(%s)", savedInstanceState == null ? "" : "saved state"));
        setContentView(R.layout.activity_play);

        playTableView = findViewById(R.id.play_table);
        TangramPuzzle goal = IntentCompat.getParcelableExtra(
                getIntent(), EXTRA_PUZZLE_GOAL, TangramPuzzle.class);
        playTableView.setSolution(goal);
        setUpDropTarget();
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
    }
}