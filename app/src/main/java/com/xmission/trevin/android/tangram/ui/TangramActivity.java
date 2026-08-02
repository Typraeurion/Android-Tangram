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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.TangramPreferences;

/**
 * Base class for all Tangram activities.  It applies the theme selected by
 * the current hint-level preference before the content view is inflated, so
 * the tangram pieces&mdash;and any other hint-styled custom graphics&mdash;
 * are colored consistently across every screen.
 *
 * <p>The day/night (light/dark) mode is handled separately and app-wide via
 * {@link androidx.appcompat.app.AppCompatDelegate#setDefaultNightMode(int)};
 * the hint-level themes all descend from the day/night
 * {@code Theme.Tangram}, so both apply together.</p>
 *
 * <p>An activity that needs a fixed theme regardless of the hint level can
 * override {@link #resolveThemeResource()}.</p>
 */
public abstract class TangramActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Must precede super.onCreate (and setContentView) so the whole view
        // hierarchy, including PlayTableView, reads the chosen theme.
        setTheme(resolveThemeResource());
        super.onCreate(savedInstanceState);
    }

    /**
     * @return the style resource to apply to this activity.  The default is
     * the theme for the current hint level; subclasses may override to force
     * a specific subtheme.
     */
    @StyleRes
    protected int resolveThemeResource() {
        return themeForHintLevel(
                TangramPreferences.getInstance(this).getHintLevel());
    }

    /**
     * Map a hint level to the theme that colors the tangram pieces (and any
     * other hint-styled graphics):
     * <ul>
     *   <li>{@code OPAQUE} &rarr; all one color, no outline (hardest);</li>
     *   <li>{@code HINT} &rarr; all one color with outlines;</li>
     *   <li>{@code SOLVE} &rarr; the fully, distinctly colored solution.</li>
     * </ul>
     *
     * @param level the current hint level
     * @return the corresponding style resource
     */
    @StyleRes
    protected static int themeForHintLevel(
            @NonNull TangramPreferences.HintLevel level) {
        return switch (level) {
            case HINT -> R.style.Theme_Tangram_Hinted;
            case SOLVE -> R.style.Theme_Tangram;
            default -> R.style.Theme_Tangram_Puzzle; // OPAQUE
        };
    }
}