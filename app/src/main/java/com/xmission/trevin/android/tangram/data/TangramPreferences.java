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
package com.xmission.trevin.android.tangram.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wrapper around @link{SharedPreferences} which provides accessors
 * to Tangram&rsquo;s preferences.  We also can be configured to
 * provide mock data or spy on the actual preferences for
 * testing purposes.
 *
 * @author Trevin Beattie
 */
public class TangramPreferences
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOG_TAG = "TangramPreferences";

    /** Preferences tag for the Tangram application */
    public static final String TANGRAM_PREFERENCES = "TangramPreferences";

    /** Label for the preferences option "UI Theme" */
    public static final String PREF_UI_THEME = "UITheme";

    /** Label for the preferences option "Piece Coloring" */
    public static final String PREF_PIECE_COLOR = "PieceColoring";

    /** Label for the preferences option "Hint Level" */
    public static final String PREF_HINT_LEVEL = "HintLevel";

    /** Label for the preferences option for the back button position */
    public static final String PREF_BACK_BUTTON_CORNER = "BackButtonCorner";

    /** Label for the preferences option for the puzzle goal position */
    public static final String PREF_GOAL_CORNER = "GoalCorner";

    /** Label for the preferences option for the "Save Puzzle" button position */
    public static final String PREF_SAVE_BUTTON_CORNER = "SaveButtonCorner";

    /**
     * Label for the preferences option for
     * the location of user-supplied puzzles
     */
    public static final String PREF_USER_PUZZLES_DIR = "UserPuzzlesDir";

    /** The singleton Tangram preferences object */
    private static TangramPreferences instance = null;

    /** The actual shared preferences we&rsquo;re relaying through */
    private final SharedPreferences prefs;

    /**
     * Handler for calling back observers; all observer calls
     * <i>must</i> be done on the main UI thread.
     */
    private final Handler uiHandler;

    /** Values for the UI theme */
    public enum UITheme {
        /**
         * Light mode (black text on white background);
         * this was the only theme available in older versions.
         */
        LIGHT,
        /** Dark mode (white text on black background) */
        DARK,
        /** Use the system-wide light/dark setting (default) */
        SYSTEM_DEFAULT
    }

    /** Cache the old (or default) UI theme */
    private UITheme oldTheme = UITheme.SYSTEM_DEFAULT;

    /** Values for coloring pieces and giving hints for a puzzle */
    public enum PiecesTheme {
        /** No hinting; pieces are the same color with no outline. */
        OPAQUE,
        /** Light hint: pieces are the same color but have outlines. */
        OUTLINE,
        /** Solution: pieces have distinct colors and outlines. */
        MULTICOLOR
    }

    /** Cache the old (or default) piece coloring */
    private PiecesTheme oldPieceColoring = PiecesTheme.OUTLINE;

    /** Cache the old (or default) hint level */
    private PiecesTheme oldHint = PiecesTheme.OPAQUE;

    /** Values for corners */
    public enum Corner {
        /** Top-left corner */
        TOP_LEFT(Gravity.TOP | Gravity.LEFT),
        /** Top-right corner */
        TOP_RIGHT(Gravity.TOP | Gravity.RIGHT),
        /** Bottom-left corner */
        BOTTOM_LEFT(Gravity.BOTTOM | Gravity.LEFT),
        /** Bottom-right corner */
        BOTTOM_RIGHT (Gravity.BOTTOM | Gravity.RIGHT);

        /** Layout gravity for the frame used in this corner */
        private final int gravity;

        Corner(int gravity) {
            this.gravity = gravity;
        }

        /** @return the gravity for this corner */
        public int getGravity() {
            return gravity;
        }
    }

    /** Cache the old (or default) back button corner */
    private Corner oldBackButtonCorner = Corner.TOP_LEFT;

    /** Cache the old (or default) goal corner */
    private Corner oldGoalCorner = Corner.TOP_RIGHT;

    /** Cache the old (or default) save button corner */
    private Corner oldSaveButtonCorner = Corner.BOTTOM_RIGHT;

    /**
     * Definition of a listener to call when the UI theme has changed.
     */
    public interface OnUIThemeChangedListener {
        /**
         * Called when the UI theme has been changed.
         *
         * @param newTheme the theme that was set
         */
        void onUIThemeChanged(@NonNull UITheme newTheme);
    }

    /**
     * Definition of a listener to call when the piece coloring has changed.
     */
    public interface OnPieceColoringChangedListener {
        /**
         * Called when the piece coloring has been changed.
         *
         * @param newColor the piece coloring that was set
         */
        void onPieceColoringChanged(@NonNull PiecesTheme newColor);
    }

    /**
     * Definition of a listener to call when the hint level has changed.
     */
    public interface OnHintLevelChangedListener {
        /**
         * Called when the hint level has been changed.
         *
         * @param newHint the hint level that was set
         */
        void onHintLevelChanged(@NonNull PiecesTheme newHint);
    }

    /**
     * Definition of a listener to call when the position of either the
     * back button, goal, or save button has changed.
     */
    public interface OnCornerChangedListener {
        /**
         * Called when the position of corner buttons has changed.
         *
         * @param key the name of the button/view whose corner has changed.
         * @param position the corner that it should be in.
         */
        void onCornerChanged(@NonNull String key, @NonNull Corner position);
    }

    /**
     * Registered listeners for changes to the UI theme.
     */
    private final List<OnUIThemeChangedListener> uiThemeListeners =
            new ArrayList<>();

    /**
     * Registered listeners for changes to the piece coloring.
     */
    private final List<OnPieceColoringChangedListener> pieceColoringListeners =
            new ArrayList<>();

    /**
     * Registered listeners for changes to the hint level.
     */
    private final List<OnHintLevelChangedListener> hintLevelListeners =
            new ArrayList<>();

    /**
     * Registered listeners for changes to the corner positions.
     */
    private final List<OnCornerChangedListener> cornerListeners =
            new ArrayList<>();

    /**
     * Instantiate Tangram preferences for a calling context.
     */
    private TangramPreferences(@NonNull Context context) {
        this(context.getSharedPreferences(TANGRAM_PREFERENCES, Context.MODE_PRIVATE),
                new Handler(Looper.getMainLooper()));
    }

    /**
     * Instantiate Tangram preferences with a given {@link SharedPreferences}
     * object.  There are two paths by which this can be called:
     * from the {@link Context} constructor it runs on the Android
     * system and uses the system&rsquo;s shared preferences and
     * a {@link Handler} for making calls to the UI thread.
     * From {@link #getInstance} with a {@code null} context it is
     * assumed to be running from stand-alone unit tests.
     *
     * @param otherPrefs the shared preferences to wrap
     * @param handler the handler for making observer callbacks on
     * the UI thread, or {@code null} to make callbacks on the same
     * thread.
     */
    private TangramPreferences(@NonNull SharedPreferences otherPrefs,
                               @Nullable Handler handler) {
        Log.d(LOG_TAG, String.format(Locale.US,
                "Creating %s with underlying %s", LOG_TAG,
                otherPrefs.getClass().getSimpleName()));
        prefs = otherPrefs;
        // Initialize any existing preference values
        oldTheme = getUITheme();
        oldHint = getHintLevel();
        prefs.registerOnSharedPreferenceChangeListener(this);
        uiHandler = handler;
    }

    /**
     * Set the {@link SharedPreferences} that TangramPreferences should
     * wrap.  This is meant for unit testing, where the test class
     * provides a mock or spy SharedPreferences.
     */
    public static void setSharedPreferences(@NonNull SharedPreferences prefs) {
        Log.d(LOG_TAG, String.format(Locale.US,
                ".setSharedPreferences(%s)",
                prefs.getClass().getSimpleName()));
        if (instance != null) {
            if (instance.prefs == prefs)
                return;
            // We need to unregister the old listener
            // before replacing the preferences.
            instance.prefs.unregisterOnSharedPreferenceChangeListener(instance);
        }
        // When running instrumented tests, make sure we have a
        // handler to run observer callbacks on the UI thread.
        Handler handler = null;
        try {
            Class<?> looperClass = Class.forName("android.os.Looper");
            Method getMainLooper = looperClass.getDeclaredMethod("getMainLooper");
            Looper looper = (Looper) getMainLooper.invoke(looperClass);
            Class<?> handlerClass = Class.forName("android.os.Handler");
            Constructor<?> cons = handlerClass.getDeclaredConstructor(Looper.class);
            handler = (Handler) cons.newInstance(looper);
        } catch (Exception e) {
            // We must not be running on Android; ignore
        }
        instance = new TangramPreferences(prefs, handler);
    }

    /**
     * Get the application preferences.
     *
     * @param context the context for which shared preferences is needed.
     * This may be null <i>only</i> for stand-alone tests that don&rsquo;t
     * run on an Android device.
     * @return a shared instance of TangramPreferences
     */
    public static TangramPreferences getInstance(@Nullable Context context) {
        Log.d(LOG_TAG, String.format(Locale.US,
                "Getting TangramPreferences instance for %s",
                (context == null) ? null
                        : context.getClass().getSimpleName()));
        if (instance == null) {
            if (context != null)
                instance = new TangramPreferences(context);
            else
                throw new IllegalStateException("TangramPreferences was"
                        + " not initialized for use without a context");
        }
        return instance;
    }

    /**
     * Wrapper around {@link SharedPreferences.Editor} for Tangram
     * preferences.  This allows callers to modify any number of
     * preferences in a single operation.  The caller <i>must</i>
     * end the call chain with {@link Editor#finish}.
     */
    public class Editor {

        private final SharedPreferences.Editor actualEditor;

        Editor() {
            actualEditor = prefs.edit();
        }

        /**
         * Apply all pending changes to the Tangram preferences.
         */
        public void finish() {
            actualEditor.apply();
        }

        /**
         * Change the UI theme.
         *
         * @param theme the type of theme to use
         *
         * @return this Editor for chaining
         */
        public Editor setUITheme(UITheme theme) {
            actualEditor.putString(PREF_UI_THEME, theme.name());
            return this;
        }

        /**
         * Change the theme of the piece colors for the play area
         * and app icons
         *
         * @param theme the type of theme to use
         *
         * @return this Editor for chaining
         */
        public Editor setPieceColoring(PiecesTheme theme) {
            actualEditor.putString(PREF_PIECE_COLOR, theme.name());
            return this;
        }

        /**
         * Change the hint level.
         *
         * @param level the hint level to use
         *
         * @return this Editor for chaining
         */
        public Editor setHintLevel(PiecesTheme level) {
            actualEditor.putString(PREF_HINT_LEVEL, level.name());
            return this;
        }

        /**
         * Change the corner for the Back button.
         *
         * @param corner the corner to use
         *
         * @return this Editor for chaining
         */
        public Editor setBackButtonCorner(Corner corner) {
            actualEditor.putString(PREF_BACK_BUTTON_CORNER, corner.name());
            return this;
        }

        /**
         * Change the corner for the puzzle goal.
         *
         * @param corner the corner to use
         *
         * @return this Editor for chaining
         */
        public Editor setGoalCorner(Corner corner) {
            actualEditor.putString(PREF_GOAL_CORNER, corner.name());
            return this;
        }

        /**
         * Change the corner for the Save button.
         *
         * @param corner the corner to use
         *
         * @return this Editor for chaining
         */
        public Editor setSaveButtonCorner(Corner corner) {
            actualEditor.putString(PREF_SAVE_BUTTON_CORNER, corner.name());
            return this;
        }

        /**
         * Set or change the directory where user-supplied puzzles are stored.
         *
         * @param newDir the directory to set, or {@code null} to clear
         *
         * @return this Editor for chaining
         */
        public Editor setUserPuzzlesDir(@Nullable String newDir) {
            if (newDir != null)
                actualEditor.putString(PREF_USER_PUZZLES_DIR, newDir);
            else
                actualEditor.remove(PREF_USER_PUZZLES_DIR);
            return this;
        }

    }

    /**
     * @return an {@link Editor} for updating multiple preferences.  The
     * caller <i>must</i> finish the method chain with {@link Editor#finish()}.
     */
    public Editor edit() {
        return new Editor();
    }

    /** @return the current UI theme */
    public @NonNull UITheme getUITheme() {
        String themeName = prefs.getString(PREF_UI_THEME,
                UITheme.SYSTEM_DEFAULT.name());
        try {
            return UITheme.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, String.format(Locale.US,
                    "Invalid UI theme (%s) in preferences", themeName));
            return UITheme.SYSTEM_DEFAULT;
        }
    }

    /**
     * Change the UI theme.
     *
     * @param theme the type of theme to use
     */
    public void setUITheme(@NonNull UITheme theme) {
        edit().setUITheme(theme).finish();
    }

    /** @return the current piece coloring theme */
    public @NonNull PiecesTheme getPieceColoring() {
        String levelName = prefs.getString(PREF_PIECE_COLOR,
                PiecesTheme.OPAQUE.name());
        try {
            return PiecesTheme.valueOf(levelName);
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, String.format(Locale.US,
                    "Invalid piece coloring (%s) in preferences", levelName));
            return PiecesTheme.OUTLINE;
        }
    }

    /**
     * Change the piece coloring
     */
    public void setPieceColoring(@NonNull PiecesTheme level) {
        edit().setPieceColoring(level).finish();
    }

    /** @return the current hint level */
    public @NonNull PiecesTheme getHintLevel() {
        String levelName = prefs.getString(PREF_HINT_LEVEL,
                PiecesTheme.OPAQUE.name());
        try {
            return PiecesTheme.valueOf(levelName);
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, String.format(Locale.US,
                    "Invalid hint level (%s) in preferences", levelName));
            return PiecesTheme.OPAQUE;
        }
    }

    /**
     * Change the hint level
     *
     * @param level the hint level to use
     */
    public void setHintLevel(@NonNull PiecesTheme level) {
        edit().setHintLevel(level).finish();
    }

    /** @return the corner in which to place the Back button in PlayActivity */
    public @NonNull Corner getBackButtonCorner() {
        String cornerName = prefs.getString(PREF_BACK_BUTTON_CORNER,
                Corner.TOP_LEFT.name());
        try {
            return Corner.valueOf(cornerName);
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, String.format(Locale.US,
                    "Invalid back button corner (%s) in preferences",
                    cornerName));
            return Corner.TOP_LEFT;
        }
    }

    /**
     * Change the corner in which to place the Back button in PlayActivity.
     *
     * @param corner the corner to use
     */
    public void setBackButtonCorner(@NonNull Corner corner) {
        edit().setBackButtonCorner(corner).finish();
    }

    /** @return the corner in which to place the puzzle goal in PlayActivity */
    public @NonNull Corner getGoalCorner() {
        String cornerName = prefs.getString(PREF_GOAL_CORNER,
                Corner.TOP_RIGHT.name());
        try {
            return Corner.valueOf(cornerName);
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, String.format(Locale.US,
                    "Invalid goal corner (%s) in preferences", cornerName));
            return Corner.TOP_RIGHT;
        }
    }

    /**
     * Change the corner in which to place the puzzle goal in PlayActivity.
     *
     * @param corner the corner to use
     */
    public void setGoalCorner(@NonNull Corner corner) {
        edit().setGoalCorner(corner).finish();
    }

    /** @return the corner in which to place the Save button in PlayActivity */
    public @NonNull Corner getSaveButtonCorner() {
        String cornerName = prefs.getString(PREF_SAVE_BUTTON_CORNER,
                Corner.BOTTOM_RIGHT.name());
        try {
            return Corner.valueOf(cornerName);
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, String.format(Locale.US,
                    "Invalid save button corner (%s) in preferences", cornerName));
            return Corner.BOTTOM_RIGHT;
        }
    }

    /**
     * Change the corner in which to place the Save button in PlayActivity.
     *
     * @param corner the corner to use
     */
    public void setSaveButtonCorner(@NonNull Corner corner) {
        edit().setSaveButtonCorner(corner).finish();
    }

    /**
     * @return the directory in which user-supplied puzzles are stored,
     * or {@code null} if no such directory is set.
     */
    public @Nullable String getUserPuzzlesDir() {
        return prefs.getString(PREF_USER_PUZZLES_DIR, null);
    }

    /**
     * Set or change the directory where user-supplied puzzles are stored.
     *
     * @param newDir the directory to set, or {@code null} to clear
     */
    public void setUserPuzzlesDir(@Nullable String newDir) {
        edit().setUserPuzzlesDir(newDir).finish();
    }

    /**
     * Register a listener for changes to the UI theme.
     *
     * @param listener the listener to register
     */
    public void registerUIThemeListener(
            @NonNull OnUIThemeChangedListener listener) {
        uiThemeListeners.add(listener);
    }

    /**
     * Remove a callback for changes to the UI theme.
     *
     * @param listener the listener to remove
     */
    public void unregisterUIThemeListener(
            @NonNull OnUIThemeChangedListener listener) {
        uiThemeListeners.remove(listener);
    }

    /**
     * Register a listener for changes to the piece coloring.
     *
     * @param listener the listener to register
     */
    public void registerPieceColoringListener(
            @NonNull OnPieceColoringChangedListener listener) {
        pieceColoringListeners.add(listener);
    }

    /**
     * Remove a callback for changes to the piece coloring.
     *
     * @param listener the listener to remove
     */
    public void unregisterPieceColoringListener(
            @NonNull OnPieceColoringChangedListener listener) {
        pieceColoringListeners.remove(listener);
    }

    /**
     * Register a listener for changes to the hint level.
     *
     * @param listener the listener to register
     */
    public void registerHintLevelListener(
            @NonNull OnHintLevelChangedListener listener) {
        hintLevelListeners.add(listener);
    }

    /**
     * Remove a callback for changes to the hint level.
     *
     * @param listener the listener to remove
     */
    public void unregisterHintLevelListener(
            @NonNull OnHintLevelChangedListener listener) {
        hintLevelListeners.remove(listener);
    }

    /**
     * Register a listener for changes to the corner positions.
     *
     * @param listener the listener to register
     */
    public void registerCornerListener(OnCornerChangedListener listener) {
        cornerListeners.add(listener);
    }

    /**
     * Remove a callback for changes to a corner.
     *
     * @param listener the listener to remove
     */
    public void unregisterCornerListener(OnCornerChangedListener listener) {
        cornerListeners.remove(listener);
    }

    /**
     * Call back the {@code onUIThemeChanged} method of all UI theme listeners.
     */
    private class UIThemeCallbackRunner implements Runnable {
        private final UITheme newTheme;
        UIThemeCallbackRunner(UITheme newTheme) {
            this.newTheme = newTheme;
        }
        @Override
        public void run() {
            synchronized(uiThemeListeners) {
                for (OnUIThemeChangedListener listener : uiThemeListeners) {
                    listener.onUIThemeChanged(newTheme);
                }
            }
        }
    }

    /**
     * Call back the {@code onPieceColoringChanged} method of all
     * piece coloring listeners.
     */
    private class PieceColoringCallbackRunner implements Runnable {
        private final PiecesTheme newColor;
        PieceColoringCallbackRunner(PiecesTheme newColor) {
            this.newColor = newColor;
        }
        @Override
        public void run() {
            synchronized(pieceColoringListeners) {
                for (OnPieceColoringChangedListener listener : pieceColoringListeners) {
                    listener.onPieceColoringChanged(newColor);
                }
            }
        }
    }

    /**
     * Call back the {@code onHintLevelChanged} method of all hint level listeners.
     */
    private class HintLevelCallbackRunner implements Runnable {
        private final PiecesTheme newLevel;
        HintLevelCallbackRunner(PiecesTheme newLevel) {
            this.newLevel = newLevel;
        }
        @Override
        public void run() {
            synchronized(hintLevelListeners) {
                for (OnHintLevelChangedListener listener : hintLevelListeners) {
                    listener.onHintLevelChanged(newLevel);
                }
            }
        }
    }

    /**
     * Call back the {@code onCornerChanged} method of all corner listeners.
     */
    private class CornerCallbackRunner implements Runnable {
        private final String key;
        private final Corner position;
        CornerCallbackRunner(String key, Corner position) {
            this.key = key;
            this.position = position;
        }
        @Override
        public void run() {
            synchronized(cornerListeners) {
                for (OnCornerChangedListener listener : cornerListeners) {
                    listener.onCornerChanged(key, position);
                }
            }
        }
    }

    /**
     * When a shared preference has been changed, notify any registered
     * listener for that preference.  This ensures callbacks are done
     * on the UI thread if we&rsquo;re running in an Android context.
     */
    @Override
    public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        Log.d(LOG_TAG, String.format(Locale.US,
                "onSharedPreferenceChanged(%s)", key));
        switch (key) {
            case PREF_UI_THEME:
                UITheme newTheme = getUITheme();
                if (newTheme != oldTheme) {
                    UIThemeCallbackRunner uiRunner =
                            new UIThemeCallbackRunner(newTheme);
                    if (uiHandler == null)
                        uiRunner.run();
                    else
                        uiHandler.post(uiRunner);
                    oldTheme = newTheme;
                }
                break;

            case PREF_PIECE_COLOR:
                PiecesTheme newColor = getPieceColoring();
                if (newColor != oldPieceColoring) {
                    PieceColoringCallbackRunner colorRunner =
                            new PieceColoringCallbackRunner(newColor);
                    if (uiHandler == null)
                        colorRunner.run();
                    else
                        uiHandler.post(colorRunner);
                    oldPieceColoring = newColor;
                }
                break;

            case PREF_HINT_LEVEL:
                PiecesTheme newHint = getHintLevel();
                if (newHint != oldHint) {
                    HintLevelCallbackRunner hintRunner =
                            new HintLevelCallbackRunner(newHint);
                    if (uiHandler == null)
                        hintRunner.run();
                    else
                        uiHandler.post(hintRunner);
                    oldHint = newHint;
                }
                break;

            case PREF_BACK_BUTTON_CORNER:
            case PREF_GOAL_CORNER:
            case PREF_SAVE_BUTTON_CORNER:
                Corner oldCorner = switch(key) {
                    case PREF_BACK_BUTTON_CORNER -> oldBackButtonCorner;
                    case PREF_GOAL_CORNER -> oldGoalCorner;
                    case PREF_SAVE_BUTTON_CORNER -> oldSaveButtonCorner;
                    // Unreachable
                    default -> null;
                };
                Corner newCorner = switch(key) {
                    case PREF_BACK_BUTTON_CORNER -> getBackButtonCorner();
                    case PREF_GOAL_CORNER -> getGoalCorner();
                    case PREF_SAVE_BUTTON_CORNER -> getSaveButtonCorner();
                    // Unreachable
                    default -> null;
                };
                if (newCorner != oldCorner) {
                    CornerCallbackRunner cornerRunner =
                            new CornerCallbackRunner(key, newCorner);
                    if (uiHandler == null)
                        cornerRunner.run();
                    else
                        uiHandler.post(cornerRunner);
                }
                switch (key) {
                    case PREF_BACK_BUTTON_CORNER:
                        oldBackButtonCorner = newCorner;
                        break;
                    case PREF_GOAL_CORNER:
                        oldGoalCorner = newCorner;
                        break;
                    case PREF_SAVE_BUTTON_CORNER:
                        oldSaveButtonCorner = newCorner;
                        break;
                }

            case PREF_USER_PUZZLES_DIR:
                // No callback interface defined at this time.
                break;

            default:
                Log.w(LOG_TAG, String.format(Locale.US,
                        "Received changed notice for unhandled preference %s",
                        key));
        }
    }

}
