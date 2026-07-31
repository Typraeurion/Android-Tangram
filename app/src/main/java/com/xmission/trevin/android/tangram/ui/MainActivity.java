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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.PuzzleLibrary;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, String.format(Locale.US,
                "onCreate(%s)", savedInstanceState == null ? "" : "saved state"));
        setContentView(R.layout.activity_main);
        WindowInsetsUtil.applySafeAreaPadding(this);

        Button button = findViewById(R.id.MainButtonLibrary);
        // Verify whether there are any puzzles available
        PuzzleLibrary library = PuzzleLibrary.getInstance();
        if (!library.isInitialized()) try {
            library.loadPuzzles(this);
            if (library.size() > 0)
                button.setOnClickListener(new OnLibrarySelected());
            else
                // No puzzles found; hide the library button
                button.setVisibility(View.GONE);
        } catch (Exception e) {
            // The error should have been logged by the library;
            // just hide the library button
            button.setVisibility(View.GONE);
        }
        button = findViewById(R.id.MainButtonSketch);
        button.setOnClickListener(new OnFreePlaySelected());
        button = findViewById(R.id.MainButtonPreferences);
        button.setOnClickListener(new OnPreferencesSelected());
        button = findViewById(R.id.MainButtonAbout);
        button.setOnClickListener(new OnAboutSelected());
    }

    /**
     * Called when the user selects the puzzle library
     */
    private class OnLibrarySelected implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Log.d(LOG_TAG, "OnLibrarySelected.onClick");
            // To Do: Start the puzzle library activity
        }
    }

    /**
     * Called when the user selects the free-form mode
     */
    private class OnFreePlaySelected implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Log.d(LOG_TAG, "OnFreePlaySelected.onClick");
            Intent intent = PlayActivity.createIntent(MainActivity.this, null);
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
            // To Do: Start PlayActivity with the saved puzzle
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
