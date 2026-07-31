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

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Helpers for laying activities out edge-to-edge without letting their
 * content fall under system UI.
 *
 * <p>On recent Android versions the app is drawn edge-to-edge by default:
 * the status bar, navigation bar, and any display cutout overlap the
 * window rather than pushing it inward.  These helpers restore a safe area
 * by padding the content for those insets.</p>
 */
public final class WindowInsetsUtil {

    private WindowInsetsUtil() {
    }

    /**
     * Lay the activity out edge-to-edge and pad its content view so that no
     * part of the layout sits under the status bar, navigation bar, or a
     * display cutout.  Call after {@code setContentView}.
     *
     * @param activity the activity to adjust
     */
    public static void applySafeAreaPadding(@NonNull Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        View content = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets safe = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return windowInsets;
        });
    }
}