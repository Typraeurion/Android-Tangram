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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.documentfile.provider.DocumentFile;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.TangramPreferences;

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
     * Arbitrary request code for selecting a directory in which to save
     * user-created puzzles from Android&rqsuo;s Open Document intent
     */
    private static final int SAF_PICK_PUZZLE_DIRECTORY = 17;

    private TangramPreferences prefs;

    private final Map<TangramPreferences.Corner,Spinner> cornerSpinnerMap
            = new HashMap<>();

    /**
     * Functional interface for getting a corner preference.
     * We have to define this instead of using Java&rsquo;s
     * java.util.function package because that isn&rsquo;t supported
     * on API levels &le; 23.
     */
    @FunctionalInterface
    private interface TangramPreferencesGetter {
        TangramPreferences.Corner getCorner(TangramPreferences prefs);
    }

    /**
     * Functional interface for setting a corner preference.
     * We have to define this instead of using Java&rsquo;s
     * java.util.function package because that isn&rsquo;t supported
     * on API levels &le; 23.
     */
    @FunctionalInterface
    private interface TangramPreferencesSetter {
        TangramPreferences.Editor setCorner(
                TangramPreferences.Editor editor,
                TangramPreferences.Corner corner);
    }

    /**
     * Enumeration of controls that can be moved into different corners
     * of the {@link PlayTableView}; the order of these must match
     * the CornerButtonList string array resource.
     */
    private enum CornerControl {
        NONE((p) -> null, (e, c) -> e),
        EXIT(TangramPreferences::getBackButtonCorner,
                TangramPreferences.Editor::setBackButtonCorner),
        GOAL(TangramPreferences::getGoalCorner,
                TangramPreferences.Editor::setGoalCorner),
        SAVE(TangramPreferences::getSaveButtonCorner,
                TangramPreferences.Editor::setSaveButtonCorner);

        private final TangramPreferencesGetter prefGetter;
        private final TangramPreferencesSetter prefSetter;

         CornerControl(TangramPreferencesGetter getter,
                       TangramPreferencesSetter setter) {
            prefGetter = getter;
            prefSetter = setter;
        }

        public TangramPreferences.Corner getCorner(TangramPreferences prefs) {
            return prefGetter.getCorner(prefs);
        }

        public TangramPreferences.Editor setCorner(
                TangramPreferences.Editor editor,
                TangramPreferences.Corner corner) {
            return prefSetter.setCorner(editor, corner);
        }

    }

    /**
     * Map showing which hovering control is in each corner position.
     * This is the reverse of {@link #controlCornerMap}.
     */
    private final Map<TangramPreferences.Corner, CornerControl> cornerControlMap
            = new HashMap<>();

    /**
     * Map showing the corner position of each hovering control,
     * including {@link CornerControl#NONE}, so if a corner's
     * control is changed we know which corner to swap it with.
     * This is the reverse of {@link #cornerControlMap}.
     */
    private final Map<CornerControl, TangramPreferences.Corner> controlCornerMap
            = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, String.format(Locale.US, "onCreate(%s)",
                savedInstanceState == null ? "" : "saved state"));
        setContentView(R.layout.activity_preferences);
        WindowInsetsUtil.applySafeAreaPadding(this);

        prefs = TangramPreferences.getInstance(this);

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

        cornerSpinnerMap.put(TangramPreferences.Corner.TOP_LEFT,
                findViewById(R.id.PreferencesSpinnerCornerTopLeft));
        cornerSpinnerMap.put(TangramPreferences.Corner.TOP_RIGHT,
                findViewById(R.id.PreferencesSpinnerCornerTopRight));
        cornerSpinnerMap.put(TangramPreferences.Corner.BOTTOM_LEFT,
                findViewById(R.id.PreferencesSpinnerCornerBottomLeft));
        cornerSpinnerMap.put(TangramPreferences.Corner.BOTTOM_RIGHT,
                findViewById(R.id.PreferencesSpinnerCornerBottomRight));
        // Initialize all corner spinners to "None" before looking at
        // where the controls are placed.
        for (Spinner spinner : cornerSpinnerMap.values())
            spinner.setSelection(CornerControl.NONE.ordinal());
        List<TangramPreferences.Corner> unusedCorners =
                new ArrayList<>(TangramPreferences.Corner.values().length);
        Collections.addAll(unusedCorners, TangramPreferences.Corner.values());
        List<CornerControl> controlCollisions = new ArrayList<>(2);
        TangramPreferences.Corner corner = prefs.getBackButtonCorner();
        cornerSpinnerMap.get(corner)
                .setSelection(CornerControl.EXIT.ordinal());
        cornerControlMap.put(corner, CornerControl.EXIT);
        controlCornerMap.put(CornerControl.EXIT, corner);
        unusedCorners.remove(corner);
        // From this point we need to check for collisions
        corner = prefs.getSaveButtonCorner();
        if (controlCornerMap.containsKey(corner)) {
            Log.w(LOG_TAG, String.format(Locale.US,
                    "%s is set to the same corner as %s",
                    CornerControl.SAVE, controlCornerMap.get(corner)));
            controlCollisions.add(CornerControl.SAVE);
        } else {
            cornerSpinnerMap.get(corner)
                    .setSelection(CornerControl.SAVE.ordinal());
            cornerControlMap.put(corner, CornerControl.SAVE);
            controlCornerMap.put(CornerControl.SAVE, corner);
            unusedCorners.remove(corner);
        }
        corner = prefs.getGoalCorner();
        if (controlCornerMap.containsKey(corner)) {
            Log.w(LOG_TAG, String.format(Locale.US,
                    "%s is set to the same corner as %s",
                    CornerControl.GOAL, controlCornerMap.get(corner)));
        } else {
            cornerSpinnerMap.get(corner)
                    .setSelection(CornerControl.GOAL.ordinal());
            cornerControlMap.put(corner, CornerControl.GOAL);
            controlCornerMap.put(CornerControl.GOAL, corner);
            unusedCorners.remove(corner);
        }
        for (CornerControl control : controlCollisions) {
            // Move this control to an unused corner
            corner = unusedCorners.remove(unusedCorners.size() - 1);
            cornerSpinnerMap.get(corner)
                    .setSelection(control.ordinal());
            cornerControlMap.put(corner, control);
            controlCornerMap.put(control, corner);
        }
        // There should be just one unused corner left, which belongs to NONE
        if (!unusedCorners.isEmpty()) {
            corner = unusedCorners.remove(0);
            cornerControlMap.put(corner, CornerControl.NONE);
            controlCornerMap.put(CornerControl.NONE, corner);
        }
        CornerControlChangeListener cornerListener =
                new CornerControlChangeListener();
        for (Spinner spinner : cornerSpinnerMap.values())
            spinner.setOnItemSelectedListener(cornerListener);

        Button userDirButton = findViewById(R.id.PreferencesButtonSaveFolder);
        if (prefs.getUserPuzzlesDir() == null) {
            userDirButton.setText(R.string.PrefsTextNoUserDir);
        } else {
            DocumentFile df = DocumentFile.fromTreeUri(this,
                    Uri.parse(prefs.getUserPuzzlesDir()));
            userDirButton.setText(df.getName());
        }
        userDirButton.setOnClickListener(new DirectoryButtonListener());

        ImageButton backButton = findViewById(R.id.PreferencesButtonBack);
        backButton.setOnClickListener(v -> finish());
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
        }
    }

    /**
     * Called when the user changes which control is assigned to a
     * given corner.  We need to swap it with the control that used
     * to be in this corner and put the other control in the chosen
     * control&rsquo;s previous corner.
     */
    private class CornerControlChangeListener
            implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int position, long id) {
            TangramPreferences.Corner newCorner = getCorner(parent);
            if (newCorner == null)
                return;
            CornerControl newControl = CornerControl.values()[position];
            TangramPreferences.Corner oldCorner =
                    controlCornerMap.get(newControl);
            CornerControl oldControl = cornerControlMap.get(newCorner);
            // This shouldn't happen, but...
            if (oldControl == null)
                oldControl = CornerControl.NONE;
            if ((newCorner == oldCorner) && (newControl == oldControl))
                // No change
                return;
            Log.d(LOG_TAG, String.format(Locale.US,
                    "CornerControlChangeListener.onItemSelected(%s, %s);"
                            + " swapping %s to %s",
                    newCorner, newControl, oldControl, oldCorner));
            cornerControlMap.put(newCorner, newControl);
            controlCornerMap.put(newControl, newCorner);
            cornerControlMap.put(oldCorner, oldControl);
            controlCornerMap.put(oldControl, oldCorner);
            TangramPreferences.Editor editor = prefs.edit();
            newControl.setCorner(editor, newCorner);
            oldControl.setCorner(editor, oldCorner);
            editor.finish();
            // After swapping in the preferences, update the
            // other spinner to show the swap.  This will
            // result in another Item Selected call which we
            // should ignore since its preference is up to date.
            cornerSpinnerMap.get(oldCorner).setSelection(oldControl.ordinal());
        }

        /**
         * If a selection is cleared, restore it.
         */
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            TangramPreferences.Corner corner = getCorner(parent);
            if (corner == null)
                return;
            CornerControl control = cornerControlMap.get(corner);
            // This shouldn't happen, but...
            if (control == null)
                control = CornerControl.NONE;
            parent.setSelection(control.ordinal());
        }

        /**
         * Determine which corner this spinner represents.
         *
         * @param spinner the spinner that was selected
         *
         * @return the corner that the spinner is in
         */
        private TangramPreferences.Corner getCorner(AdapterView<?> spinner) {
            for (TangramPreferences.Corner corner : cornerSpinnerMap.keySet()) {
                if (cornerSpinnerMap.get(corner) == spinner)
                    return corner;
            }
            Log.w(LOG_TAG, String.format(Locale.US,
                    "CornerControlChangeListener.getCorner(%s):"
                            + " Unknown spinner", spinner));
            return null;
        }
    }

    /**
     * Called when the user clicks the puzzle directory button.
     */
    private class DirectoryButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            // To Do: present a directory chooser
            Intent chooseDirActivity =
                    new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            chooseDirActivity.addCategory(Intent.CATEGORY_DEFAULT);
            // The chosen directory must be writable, and
            // permissions persist across app restarts.
            chooseDirActivity.setFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                    + Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    + Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // FIXME: deprecated method
            startActivityForResult(Intent.createChooser(chooseDirActivity,
                    getString(R.string.PrefsDialogUserDirTitle)),
                    SAF_PICK_PUZZLE_DIRECTORY);
        }
    }

    /**
     * Called when the user has selected a (new) puzzle directory.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SAF_PICK_PUZZLE_DIRECTORY) {
            Log.d(LOG_TAG, String.format(Locale.US,
                    "onActivityResult(%d): Ignoring unknown request code",
                    requestCode));
            return;
        }
        if (resultCode != RESULT_OK) {
            Log.i(LOG_TAG, String.format(Locale.US,
                    "onActivityResult(%s)", switch (resultCode) {
                        case RESULT_CANCELED -> "CANCELED";
                        case RESULT_FIRST_USER -> "FIRST_USER";
                        default -> Integer.toString(resultCode);
                    }));
            return;
        }
        Uri uri = data.getData();
        Log.d(LOG_TAG, String.format(Locale.US,
                "onActivityResult(OK, %s)", uri));
        Button userDirButton = findViewById(R.id.PreferencesButtonSaveFolder);
        if (uri == null) {
            prefs.setUserPuzzlesDir(null);
            userDirButton.setText(R.string.PrefsTextNoUserDir);
        } else {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            + Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            prefs.setUserPuzzlesDir(uri.toString());
            DocumentFile df = DocumentFile.fromTreeUri(this, uri);
            userDirButton.setText(df.getName());
        }
    }

}
