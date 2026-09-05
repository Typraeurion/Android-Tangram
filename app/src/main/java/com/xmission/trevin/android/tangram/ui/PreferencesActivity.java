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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import androidx.documentfile.provider.DocumentFile;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.CornerButtonModel.Control;
import com.xmission.trevin.android.tangram.data.PuzzleLibrary;
import com.xmission.trevin.android.tangram.data.TangramPreferences;
import com.xmission.trevin.android.tangram.util.BackgroundExecutor;
import com.xmission.trevin.android.tangram.util.FileUtils;

import java.io.IOException;
import java.util.*;

/**
 * Lets the user manage customizable settings, such as the UI theme,
 * layout, hint level, and directory for user-generated puzzles.
 *
 * @author Trevin Beattie
 */
public class PreferencesActivity extends TangramActivity {

    private static final String LOG_TAG = "PreferencesActivity";

    /**
     * Launcher for the system directory picker (Storage Access Framework
     * {@code OPEN_DOCUMENT_TREE}).  Registered up front&mdash;before the
     * activity is started&mdash;as the Activity Result API requires; this
     * replaces the deprecated {@code startActivityForResult}/
     * {@code onActivityResult} pair.
     */
    private final ActivityResultLauncher<Uri> pickPuzzleDirectory =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocumentTree(),
                    this::onPuzzleDirectoryChosen);

    private TangramPreferences prefs;

    private PuzzleLibrary library;

    /** The draggable corner-layout mock; its Goal chip follows the hint level. */
    private CornerButtonLayoutView cornerLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, String.format(Locale.US, "onCreate(%s)",
                savedInstanceState == null ? "" : "saved state"));
        setContentView(R.layout.activity_preferences);
        WindowInsetsUtil.applySafeAreaPadding(this);

        prefs = TangramPreferences.getInstance(this);
        library = PuzzleLibrary.getInstance();

        RadioGroup uiThemeGroup = findViewById(
                R.id.PreferencesRadioGroupUITheme);
        int uiRadioSelection = switch(prefs.getUITheme()) {
            case LIGHT -> R.id.PreferencesRadioButtonUILight;
            case DARK -> R.id.PreferencesRadioButtonUIDark;
            case SYSTEM_DEFAULT -> R.id.PreferencesRadioButtonUIDefault;
        };
        uiThemeGroup.check(uiRadioSelection);
        uiThemeGroup.setOnCheckedChangeListener(new UIThemeChangeListener());

        RadioGroup pieceThemeGroup = findViewById(
                R.id.PreferencesRadioGroupPieceTheme);
        int pieceRadioSelection = switch(prefs.getPieceColoring()) {
            case OPAQUE -> R.id.PreferencesRadioButtonPieceThemeSolid;
            case OUTLINE -> R.id.PreferencesRadioButtonPieceThemeOutline;
            case MULTICOLOR -> R.id.PreferencesRadioButtonPieceThemeMulticolor;
        };
        pieceThemeGroup.check(pieceRadioSelection);
        pieceThemeGroup.setOnCheckedChangeListener(new PieceColoringChangeListener());

        RadioGroup hintLevelGroup = findViewById(
                R.id.PreferencesRadioGroupHintLevel);
        int hintRadioSelection = switch(prefs.getHintLevel()) {
            case OPAQUE -> R.id.PreferencesRadioButtonHintNone;
            case OUTLINE -> R.id.PreferencesRadioButtonHintOutline;
            case MULTICOLOR -> R.id.PreferencesRadioButtonHintSolve;
        };
        hintLevelGroup.check(hintRadioSelection);
        hintLevelGroup.setOnCheckedChangeListener(new HintLevelChangeListener());

        // The corner controls are placed on a draggable mock of the play
        // field; the view resolves any collisions in the stored corners and
        // reports back through the listener when the user rearranges them.
        cornerLayout = findViewById(R.id.PreferencesCornerLayout);
        cornerLayout.initialize(prefs.getBackButtonCorner(),
                prefs.getGoalCorner(), prefs.getSaveButtonCorner());
        updateGoalChipIcon();
        cornerLayout.setOnCornerLayoutChangedListener(() ->
                prefs.edit()
                        .setBackButtonCorner(cornerLayout.getCorner(Control.EXIT))
                        .setGoalCorner(cornerLayout.getCorner(Control.GOAL))
                        .setSaveButtonCorner(cornerLayout.getCorner(Control.SAVE))
                        .finish());

        Button userDirButton = findViewById(R.id.PreferencesButtonSaveFolder);
        if (prefs.getUserPuzzlesDir() == null) {
            userDirButton.setText(R.string.PrefsTextNoUserDir);
        } else {
            Uri dirUri = Uri.parse(prefs.getUserPuzzlesDir());
            DocumentFile df = DocumentFile.fromTreeUri(this, dirUri);
            if ((df == null) || (df.getName() == null)) {
                forgetUserPuzzlesDir(FileUtils.getFriendlyName(dirUri));
            } else {
                userDirButton.setText(df.getName());
            }
        }
        userDirButton.setOnClickListener(new DirectoryButtonListener());

        ImageButton backButton = findViewById(R.id.PreferencesButtonBack);
        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Set the Goal chip&rsquo;s icon so it matches the current hint level,
     * the same way the play field&rsquo;s goal view is styled.  The goal
     * drawable&rsquo;s tangram attributes are resolved against the hint theme
     * rather than this activity&rsquo;s piece-coloring theme.
     */
    private void updateGoalChipIcon() {
        if (cornerLayout == null)
            return;
        Resources.Theme goalTheme = getResources().newTheme();
        goalTheme.applyStyle(resolveHintThemeResource(), true);
        Drawable goal = ResourcesCompat.getDrawable(
                getResources(), R.drawable.ic_goal, goalTheme);
        cornerLayout.setGoalIcon(goal);
    }

    /**
     * Called when the user changes the UI theme
     */
    private class UIThemeChangeListener
            implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(@NonNull RadioGroup group, int buttonId) {
            if (buttonId == -1) {
                Log.d(LOG_TAG, "UIThemeChangeListener.onCheckedChanged(cleared)");
                // The selection was cleared; re-select the current option.
                group.check(switch (prefs.getUITheme()) {
                    case LIGHT -> R.id.PreferencesRadioButtonUILight;
                    case DARK -> R.id.PreferencesRadioButtonUIDark;
                    case SYSTEM_DEFAULT -> R.id.PreferencesRadioButtonUIDefault;
                });
                return;
            }
            // Map the selected button ID to a UI theme
            TangramPreferences.UITheme checkedTheme;
            int nightMode;
            if (buttonId == R.id.PreferencesRadioButtonUILight) {
                checkedTheme = TangramPreferences.UITheme.LIGHT;
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (buttonId == R.id.PreferencesRadioButtonUIDark) {
                checkedTheme = TangramPreferences.UITheme.DARK;
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else if (buttonId == R.id.PreferencesRadioButtonUIDefault) {
                checkedTheme = TangramPreferences.UITheme.SYSTEM_DEFAULT;
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            } else {
                 Log.d(LOG_TAG, String.format(Locale.US,
                         "UIThemeChangeListener.onCheckedChanged(%d):"
                                 + " Ignoring unknown button ID", buttonId));
                return;
            }
            // Only act on a genuine change.  Programmatic check() calls and
            // view-state restoration re-select the stored preference; applying
            // the night mode again would recreate the activity and loop forever.
            if (checkedTheme == prefs.getUITheme()) {
                Log.d(LOG_TAG, String.format(Locale.US,
                        "UIThemeChangeListener.onCheckedChanged(%d):"
                                + " unchanged, ignoring", buttonId));
                return;
            }
            Log.d(LOG_TAG, String.format(Locale.US,
                    "UIThemeChangeListener.onCheckedChanged(%s)",
                    checkedTheme));
            prefs.setUITheme(checkedTheme);
            AppCompatDelegate.setDefaultNightMode(nightMode);
        }
    }

    /**
     * Called when the user changes the theme for general
     * piece coloring
     */
    private class PieceColoringChangeListener
            implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(@NonNull RadioGroup group, int buttonId) {
            if (buttonId == -1) {
                Log.d(LOG_TAG, "PieceColoringChangeListener.onCheckedChanged(cleared)");
                // The selection was cleared; re-select the current option.
                group.check(switch (prefs.getPieceColoring()) {
                    case OPAQUE -> R.id.PreferencesRadioButtonPieceThemeSolid;
                    case OUTLINE -> R.id.PreferencesRadioButtonPieceThemeOutline;
                    case MULTICOLOR -> R.id.PreferencesRadioButtonPieceThemeMulticolor;
                });
                return;
            }
            // Map the selected button to the piece theme
            TangramPreferences.PiecesTheme checkedTheme;
            if (buttonId == R.id.PreferencesRadioButtonPieceThemeSolid) {
                checkedTheme = TangramPreferences.PiecesTheme.OPAQUE;
            } else if (buttonId == R.id.PreferencesRadioButtonPieceThemeOutline) {
                checkedTheme = TangramPreferences.PiecesTheme.OUTLINE;
            } else if (buttonId == R.id.PreferencesRadioButtonPieceThemeMulticolor) {
                checkedTheme = TangramPreferences.PiecesTheme.MULTICOLOR;
            } else {
                Log.d(LOG_TAG, String.format(Locale.US,
                        "PieceColoringChangeListener.onCheckedChanged(%d):"
                                + " Ignoring unknown button ID", buttonId));
                return;
            }
            // Only act on a genuine change.  Programmatic check() calls and
            // view-state restoration re-select the stored theme; recreating
            // again would loop forever.
            if (checkedTheme == prefs.getPieceColoring()) {
                Log.d(LOG_TAG, String.format(Locale.US,
                        "PieceColoringChangeListener.onCheckedChanged(%d):"
                                + " unchanged, ignoring", buttonId));
                return;
            }
            Log.d(LOG_TAG, String.format(Locale.US,
                    "PieceColoringChangeListener.onCheckedChanged(%s)",
                    checkedTheme));
            prefs.setPieceColoring(checkedTheme);
            // The piece color theme is applied per-activity by TangramActivity;
            // re-create so this (and every later) screen picks it up.  A
            // plain setTheme() can't re-style an already-inflated activity.
            recreate();
        }
    }

    /**
     * Called when the user changes the hinting level
     */
    private class HintLevelChangeListener
            implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(@NonNull RadioGroup group, int buttonId) {
            if (buttonId == -1) {
                Log.d(LOG_TAG, "HintLevelChangeListener.onCheckedChanged(cleared)");
                // The selection was cleared; re-select the current option.
                group.check(switch (prefs.getHintLevel()) {
                    case OPAQUE -> R.id.PreferencesRadioButtonHintNone;
                    case OUTLINE -> R.id.PreferencesRadioButtonHintOutline;
                    case MULTICOLOR -> R.id.PreferencesRadioButtonHintSolve;
                });
                return;
            }
            // Map the selected button to the hint level
            TangramPreferences.PiecesTheme checkedLevel;
            if (buttonId == R.id.PreferencesRadioButtonHintNone) {
                checkedLevel = TangramPreferences.PiecesTheme.OPAQUE;
            } else if (buttonId == R.id.PreferencesRadioButtonHintOutline) {
                checkedLevel = TangramPreferences.PiecesTheme.OUTLINE;
            } else if (buttonId == R.id.PreferencesRadioButtonHintSolve) {
                checkedLevel = TangramPreferences.PiecesTheme.MULTICOLOR;
            } else {
                Log.d(LOG_TAG, String.format(Locale.US,
                        "HintLevelChangeListener.onCheckedChanged(%d):"
                                + " Ignoring unknown button ID", buttonId));
                return;
            }
            // Only act on a genuine change.  Programmatic check() calls and
            // view-state restoration re-select the stored level; recreating
            // again would loop forever.
            if (checkedLevel == prefs.getHintLevel()) {
                Log.d(LOG_TAG, String.format(Locale.US,
                        "HintLevelChangeListener.onCheckedChanged(%d):"
                                + " unchanged, ignoring", buttonId));
                return;
            }
            Log.d(LOG_TAG, String.format(Locale.US,
                    "HintLevelChangeListener.onCheckedChanged(%s)",
                    checkedLevel));
            prefs.setHintLevel(checkedLevel);
            // Re-theme the Goal chip so it reflects the new hint level.
            updateGoalChipIcon();
        }
    }

    /**
     * Called when the user clicks the puzzle directory button.
     */
    private class DirectoryButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            // Open the system directory picker, hinting at the currently
            // chosen directory (if any) as its starting location.  The
            // OpenDocumentTree contract builds the ACTION_OPEN_DOCUMENT_TREE
            // intent for us; persistable read/write permission for the
            // returned tree is taken in onPuzzleDirectoryChosen.
            Uri initialUri = (prefs.getUserPuzzlesDir() == null) ? null
                    : Uri.parse(prefs.getUserPuzzlesDir());
            pickPuzzleDirectory.launch(initialUri);
        }
    }

    /**
     * Called when the user has selected a (new) puzzle directory, or
     * dismissed the picker (in which case {@code uri} is {@code null} and
     * the current setting is left unchanged).
     *
     * @param uri the tree {@link Uri} of the chosen directory, or
     * {@code null} if the picker was cancelled
     */
    private void onPuzzleDirectoryChosen(@Nullable Uri uri) {
        if (uri == null) {
            Log.i(LOG_TAG, "Directory selection cancelled");
            return;
        }
        Log.d(LOG_TAG, String.format(Locale.US,
                "Directory chosen: %s", uri));
        // Persist read/write access across app restarts.
        getContentResolver().takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        prefs.setUserPuzzlesDir(uri.toString());
        Button userDirButton = findViewById(R.id.PreferencesButtonSaveFolder);
        DocumentFile df = DocumentFile.fromTreeUri(this, uri);
        userDirButton.setText((df == null) ?
                getText(R.string.PrefsTextNoUserDir) : df.getName());
        // Load the puzzles from the newly chosen directory off the main
        // thread (the Storage Access Framework can be slow).
        if (df != null)
            loadUserPuzzlesInBackground(df);
    }

    /**
     * Read the user puzzles from the chosen directory on a background
     * thread and, if anything went wrong, report it in a dialog.
     *
     * @param folder the chosen puzzle directory
     */
    private void loadUserPuzzlesInBackground(@NonNull DocumentFile folder) {
        BackgroundExecutor.runInBackground(() -> {
            List<String> errors;
            try {
                errors = library.loadUserPuzzles(this, folder);
            } catch (SecurityException e) {
                // The directory is no longer accessible; forget it.  Its
                // getName() would be null now, so derive a friendly name
                // from the URI instead.
                Log.w(LOG_TAG,
                        "Chosen puzzle directory is not accessible", e);
                String dirName = FileUtils.getFriendlyName(folder.getUri());
                runOnUiThread(() -> forgetUserPuzzlesDir(dirName));
                return;
            } catch (IOException e) {
                Log.w(LOG_TAG, "Error loading user puzzles", e);
                errors = Collections.singletonList(getString(
                        R.string.ErrorCannotReadUserFile,
                        folder.getName(), e.getMessage()));
            }
            if (errors == null || errors.isEmpty())
                return;
            List<String> toShow = errors;
            runOnUiThread(() -> showUserPuzzleErrors(toShow));
        });
    }

    /**
     * Forget the user-puzzle directory after finding it is no longer
     * accessible: clear the preference, restore the button label, and tell
     * the user.  Runs on the main thread.
     *
     * @param dirName a best-effort name for the lost directory
     */
    private void forgetUserPuzzlesDir(@NonNull String dirName) {
        prefs.setUserPuzzlesDir(null);
        Button userDirButton = findViewById(R.id.PreferencesButtonSaveFolder);
        userDirButton.setText(R.string.PrefsTextNoUserDir);
        showUserPuzzleErrors(Collections.singletonList(
                getString(R.string.ErrorUserDirRevoked, dirName)));
    }

    /**
     * Show problems encountered while loading user puzzles in a dialog.
     *
     * @param messages the messages to display
     */
    private void showUserPuzzleErrors(@NonNull List<String> messages) {
        if (isFinishing())
            return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.UserPuzzleErrorsTitle)
                .setMessage(String.join("\n\n", messages))
                .setPositiveButton(R.string.InfoButtonOK, null)
                .show();
    }

}
