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
package com.xmission.trevin.android.tangram;

import android.app.Application;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.xmission.trevin.android.tangram.data.PuzzleLibrary;
import com.xmission.trevin.android.tangram.data.TangramPreferences;

public class TangramApp extends Application {

    private static final String LOG_TAG = "TangramApp";

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate()");
        super.onCreate();

        // Skip initializing the application if we're running instrumented tests
        // because using real preferences would interfere with mock preferences
        // and reading the puzzles should be deferred to the test cases.
        try {
            Class.forName("androidx.test.InstrumentationRegistry");
            Log.d(LOG_TAG, "Instrumentation detected; skipping initialization");
        } catch (ClassNotFoundException cx) {
            // Get the current day/night mode and puzzle theme preference
            // and apply them to the application's theme
            TangramPreferences prefs = TangramPreferences.getInstance(this);
            TangramPreferences.UITheme theme = prefs.getUITheme();
            int nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            switch (theme) {
                case LIGHT:
                    nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                    break;
                case DARK:
                    nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                    break;
            }
            AppCompatDelegate.setDefaultNightMode(nightMode);

            TangramPreferences.HintLevel hinting = prefs.getHintLevel();
            int appThemeId = R.style.Theme_Tangram;
            switch (hinting) {
                case HINT:
                    appThemeId = R.style.Theme_Tangram_Hinted;
                    break;
                case SOLVE:
                    appThemeId = R.style.Theme_Tangram_Puzzle;
                    break;
            }
            setTheme(appThemeId);

            // Read in all available puzzles.
            // To Do: If the amount of puzzles is large,
            // we ought to find another way to manage them.
            try {
                PuzzleLibrary.getInstance().loadPuzzles(this);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error loading puzzles", e);
                // We should be able to continue running in sketch mode,
                // but there will be no puzzles for the user to solve.
            }
        }
    }

    @Override
    public void onLowMemory() {
        Log.d(LOG_TAG, ".onLowMemory");
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        Log.d(LOG_TAG, ".onTerminate");
        super.onTerminate();
    }

}
