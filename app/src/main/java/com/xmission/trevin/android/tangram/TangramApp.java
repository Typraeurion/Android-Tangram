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
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.documentfile.provider.DocumentFile;

import com.xmission.trevin.android.tangram.data.PuzzleLibrary;
import com.xmission.trevin.android.tangram.data.TangramPreferences;
import com.xmission.trevin.android.tangram.util.BackgroundExecutor;
import com.xmission.trevin.android.tangram.util.FileUtils;

import java.util.Collections;
import java.util.Locale;

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
            int nightMode = switch (theme) {
                case LIGHT -> AppCompatDelegate.MODE_NIGHT_NO;
                case DARK -> AppCompatDelegate.MODE_NIGHT_YES;
                default -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            };
            AppCompatDelegate.setDefaultNightMode(nightMode);

            // NOTE: the hint level's theme is NOT applied here.  An
            // Application's theme does not propagate to its activities, so
            // the piece styling is applied per-activity (see
            // PlayActivity.themeForHintLevel).

            // Read in all available puzzles.
            // To Do: If the amount of puzzles is large,
            // we ought to find another way to manage them.
            PuzzleLibrary library = PuzzleLibrary.getInstance();
            try {
                library.loadPuzzles(this);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error loading built-in puzzles", e);
                // We should be able to continue running in sketch mode,
                // but there will be no puzzles for the user to solve.
            }
            // The built-in puzzles are small and bundled in the APK, so they
            // load quickly enough to stay on the main thread (MainActivity
            // needs them the moment it appears).  User puzzles come through
            // the Storage Access Framework, which can be slow (e.g.
            // cloud-backed), so load them off the main thread.
            if (prefs.getUserPuzzlesDir() != null)
                BackgroundExecutor.runInBackground(
                        () -> loadUserPuzzlesAtStartup(prefs));
        }
    }

    /**
     * Load the user's saved puzzles from the configured directory on a
     * background thread.  Any problems (a revoked directory, unreadable or
     * invalid files) are queued in the {@link PuzzleLibrary} to be shown by
     * the first activity that can present a dialog, since an
     * {@code Application} cannot show one itself.
     *
     * @param prefs the shared preferences holding the puzzle directory
     */
    private void loadUserPuzzlesAtStartup(@NonNull TangramPreferences prefs) {
        PuzzleLibrary library = PuzzleLibrary.getInstance();
        Uri dirUri = Uri.parse(prefs.getUserPuzzlesDir());
        DocumentFile puzzlesDir = DocumentFile.fromTreeUri(this, dirUri);
        if (puzzlesDir == null)
            return;
        try {
            // loadUserPuzzles returns null when there were no problems.
            library.addPendingUserMessages(
                    library.loadUserPuzzles(this, puzzlesDir));
        } catch (SecurityException e) {
            // The permission for this directory was revoked, or the
            // directory itself was deleted; forget it.
            Log.w(LOG_TAG, String.format(Locale.US,
                    "Puzzle directory %s is no longer accessible",
                    prefs.getUserPuzzlesDir()));
            String dirName = FileUtils.getFriendlyName(dirUri);
            prefs.setUserPuzzlesDir(null);
            library.addPendingUserMessages(Collections.singletonList(
                    getString(R.string.ErrorUserDirRevoked, dirName)));
        } catch (Exception e) {
            Log.w(LOG_TAG, "Error loading user puzzles", e);
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
