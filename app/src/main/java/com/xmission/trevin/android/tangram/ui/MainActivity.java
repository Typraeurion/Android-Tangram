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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.GameState;
import com.xmission.trevin.android.tangram.data.PuzzleLibrary;
import com.xmission.trevin.android.tangram.data.TangramPreferences;
import com.xmission.trevin.android.tangram.data.TangramPreferences.PiecesTheme;

import java.util.List;
import java.util.Locale;

/**
 * The front page of the app, where the user can choose to look through
 * the library of available Tangram puzzles, start playing without any
 * particular goal, resume a previous game, or change the app settings.
 *
 * @author Trevin Beattie
 */
public class MainActivity extends TangramActivity {

    private static final String LOG_TAG = "MainActivity";

    private PuzzleLibrary library;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, String.format(Locale.US,
                "onCreate(%s)", savedInstanceState == null ? "" : "saved state"));
        setContentView(R.layout.activity_main);
        WindowInsetsUtil.applySafeAreaPadding(this);

        TangramPreferences prefs = TangramPreferences.getInstance(this);
        prefs.registerPieceColoringListener(new PieceColoringChangeListener());

        Button button = findViewById(R.id.MainButtonLibrary);
        Button button2 = findViewById(R.id.MainButtonRandom);
        // Verify whether there are any puzzles available
        library = PuzzleLibrary.getInstance();
        try {
            if (!library.isInitialized())
                library.loadPuzzles(this);
            if (library.size() > 0) {
                button.setOnClickListener(new OnLibrarySelected());
                button2.setOnClickListener(new OnRandomPuzzleSelected());
            } else {
                // No puzzles found; hide the library button
                button.setVisibility(View.GONE);
                button2.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            // The error should have been logged by the library;
            // just hide the library and random puzzle buttons
            button.setVisibility(View.GONE);
            button2.setVisibility(View.GONE);
        }
        button = findViewById(R.id.MainButtonSketch);
        button.setOnClickListener(new OnFreePlaySelected());
        // The Resume button is shown or hidden based on whether an
        // in-progress game is saved; that is (re)evaluated in onResume, which
        // also runs when returning here from PlayActivity.
        button = findViewById(R.id.MainButtonPreferences);
        button.setOnClickListener(new OnPreferencesSelected());
        button = findViewById(R.id.MainButtonAbout);
        button.setOnClickListener(new OnAboutSelected());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume()");
        // Surface any user-puzzle problems queued during a background load
        // (e.g. from the Application's startup load, which can't show a
        // dialog itself).  Registering also flushes anything already queued.
        library.setUserMessageListener(this::showUserPuzzleMessages);
        updateResumeButton();
    }

    /**
     * Show the &ldquo;Resume Puzzle In Progress&rdquo; button only when an
     * unfinished game has been saved (see {@link GameState}); otherwise hide
     * it.  Re-evaluated on every resume, including when returning from
     * {@link PlayActivity}.
     */
    private void updateResumeButton() {
        Button resumeButton = findViewById(R.id.MainButtonResume);
        if (GameState.getInstance().hasInProgressGame()) {
            resumeButton.setOnClickListener(new OnResumeSelected());
            resumeButton.setVisibility(View.VISIBLE);
        } else {
            resumeButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "onPause()");
        library.setUserMessageListener(null);
        super.onPause();
    }

    /**
     * Show queued user-puzzle messages (revoked directory, unreadable or
     * invalid files) in a single dialog.
     *
     * @param messages the messages to display
     */
    private void showUserPuzzleMessages(@NonNull List<String> messages) {
        if (messages.isEmpty() || isFinishing())
            return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.UserPuzzleErrorsTitle)
                .setMessage(String.join("\n\n", messages))
                .setPositiveButton(R.string.InfoButtonOK, null)
                .show();
    }

    /**
     * Called when the piece coloring preference has changed.
     * This requires us to re-create the activity so that the
     * color theme is applied to our tangram title and icons.
     */
    private class PieceColoringChangeListener
            implements TangramPreferences.OnPieceColoringChangedListener {
        @Override
        public void onPieceColoringChanged(@NonNull PiecesTheme newLevel) {
            Log.d(LOG_TAG, String.format(Locale.US,
                    "Piece coloring changed to %s", newLevel));
            recreate();
        }
    }

    /**
     * Called when the user selects the puzzle library
     */
    private class OnLibrarySelected implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Log.d(LOG_TAG, "OnLibrarySelected.onClick");
            startActivity(new Intent(MainActivity.this, LibraryActivity.class));
        }
    }

    /**
     * Called when the user selects a random puzzle
     */
    private class OnRandomPuzzleSelected implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Log.d(LOG_TAG, "OnRandomPuzzleSelected.onClick");
            if (library.size() <= 0) {
                view.setEnabled(false);
                return;
            }
            Intent intent = PlayActivity.createIntent(
                    MainActivity.this, library.getRandomPuzzle());
            startActivity(intent);
        }
    }

    /**
     * Called when the user selects the free-form mode
     */
    private class OnFreePlaySelected implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Log.d(LOG_TAG, "OnFreePlaySelected.onClick");
            Intent intent = PlayActivity.createIntent(
                    MainActivity.this, null);
            startActivity(intent);
        }
    }

    /**
     * Called when the user selects &ldquo;Resume Puzzle In Progress&rdquo;
     */
    private class OnResumeSelected implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Log.d(LOG_TAG, "OnResumeSelected.onClick");
            if (!GameState.getInstance().hasInProgressGame()) {
                Log.e(LOG_TAG, "No in-progress game to resume");
                findViewById(R.id.MainButtonResume).setVisibility(View.GONE);
                return;
            }
            startActivity(PlayActivity.createResumeIntent(MainActivity.this));
        }
    }

    /**
     * Called when the user clicks the Preferences button
     */
    private class OnPreferencesSelected implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Log.d(LOG_TAG, "OnPreferencesSelected.onClick");
            Intent prefsIntent = new Intent(MainActivity.this,
                    PreferencesActivity.class);
            startActivity(prefsIntent);
        }
    }

    /**
     * Called when the user clicks the About button
     */
    private class OnAboutSelected implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Log.d(LOG_TAG, "OnAboutSelected.onClick");
            new AboutDialogFragment().show(
                    getSupportFragmentManager(), AboutDialogFragment.TAG);
        }
    }

}
