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

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.TangramPreferences;

import java.util.Locale;

public class PreferencesActivity extends TangramActivity {

    private static final String LOG_TAG = "PreferencesActivity";

    private TangramPreferences prefs;

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
            default -> R.id.PreferencesRadioButtonUIDefault;
        };
        uiThemeGroup.check(uiRadioSelection);
        uiThemeGroup.setOnCheckedChangeListener(new UIThemeChangeListener());

        RadioGroup hintLevelGroup = findViewById(
                R.id.PreferencesRadioGroupHintLevel);
        int hintRadioSelection = switch(prefs.getHintLevel()) {
            case OPAQUE -> R.id.PreferencesRadioButtonHintNone;
            case HINT -> R.id.PreferencesRadioButtonHintOutline;
            default -> R.id.PreferencesRadioButtonHintSolve;
        };
        hintLevelGroup.check(hintRadioSelection);
        hintLevelGroup.setOnCheckedChangeListener(new HintLevelChangeListener());

        // To Do: Add options for setting the corner controls in the
        // play activity and for the directory in which to save user puzzles.

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
                    default -> R.id.PreferencesRadioButtonUIDefault;
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
                    case HINT -> R.id.PreferencesRadioButtonHintOutline;
                    default -> R.id.PreferencesRadioButtonHintSolve;
                });
                return;
            }
            // Map the selected button to the hint level
            TangramPreferences.HintLevel checkedLevel;
            if (buttonId == R.id.PreferencesRadioButtonHintNone) {
                checkedLevel = TangramPreferences.HintLevel.OPAQUE;
            } else if (buttonId == R.id.PreferencesRadioButtonHintOutline) {
                checkedLevel = TangramPreferences.HintLevel.HINT;
            } else if (buttonId == R.id.PreferencesRadioButtonHintSolve) {
                checkedLevel = TangramPreferences.HintLevel.SOLVE;
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
            // The hint theme is applied per-activity by TangramActivity;
            // re-create so this (and every later) screen picks it up.  A
            // plain setTheme() can't re-style an already-inflated activity.
            recreate();
        }
    }

}
