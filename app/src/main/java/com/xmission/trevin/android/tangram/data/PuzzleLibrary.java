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
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * This class manages the collection of puzzles available from the
 * application assets.
 *
 * @author Trevin Beattie
 */
public class PuzzleLibrary {

    private static final String LOG_TAG = "PuzzleLibrary";

    private static PuzzleLibrary instance = null;

    private final List<TangramPuzzle> puzzles = new ArrayList<>();

    private boolean initialized = false;

    /**
     * This is a singleton class, so we only allow one instance to be created.
     */
    private PuzzleLibrary() {
    }

    /**
     * @return the puzzle library
     */
    public static PuzzleLibrary getInstance() {
        if (instance == null)
            instance = new PuzzleLibrary();
        return instance;
    }

    /**
     * Read the contents of a JSON asset file.
     *
     * @param context the context in which the application is running
     * @param assetName the name of the file in the &ldquo;assets/&rdquo;
     * folder to read
     *
     * @return the JSON array or object from the file
     *
     * @throws IOException if the asset does not exist or cannot be read
     * @throws JSONException if the JSON is malformed, or if the content
     * is neither a {@link JSONArray} nor a {@link JSONObject}.
     */
    public @NonNull Object readJSONAsset(
            @NonNull Context context, @NonNull String assetName)
            throws IOException, JSONException {
        StringBuilder builder = new StringBuilder();
        try (InputStream iStream = context.getAssets().open(assetName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     iStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line);
        }
        Object o = new JSONTokener(builder.toString()).nextValue();
        if (!(o instanceof JSONArray) && !(o instanceof JSONObject))
            throw new JSONException("Invalid JSON in " + assetName);
        return o;
    }

    /**
     * Initialize the library by loading all of the puzzles
     * from the assets folder that are named {@code puzzles-*.json}.
     *
     * @param context the context in which the application is running
     *
     * @throws IOException if there was an error reading any of the assets
     */
    public void loadPuzzles(@NonNull Context context) throws IOException {
        final Pattern PUZZLE_PATTERN = Pattern.compile("puzzles-.*\\.json");
        puzzles.clear();
        String[] assets = context.getAssets().list("");
        if (assets == null) {
            Log.i(LOG_TAG, "No assets found");
            return;
        }
        Object json = null;
        for (String assetName : assets) {
            if (PUZZLE_PATTERN.matcher(assetName).matches()) try {
                json = readJSONAsset(context, assetName);
            } catch (IOException | JSONException e) {
                Log.e(LOG_TAG, String.format(Locale.US,
                        "Error reading assets/%s; ignoring it.",
                        assetName), e);
                continue;
            }
            if (json instanceof JSONArray jsonArray) {
                for (int i = 0; i < jsonArray.length(); i++) try {
                    TangramPuzzle puzzle = new TangramPuzzle(
                            jsonArray.getJSONObject(i));
                    puzzle.setSourceFileName("assets/" + assetName);
                    puzzles.add(puzzle);
                } catch (InvalidPuzzleException | JSONException e) {
                    Log.w(LOG_TAG, String.format(Locale.US,
                            "Entry at assets/%s[%d] is not a valid Tangram puzzle",
                            assetName, i), e);
                }
            } else if (json instanceof JSONObject jsonObject) try {
                TangramPuzzle puzzle = new TangramPuzzle(jsonObject);
                puzzle.setSourceFileName("asset/" + assetName);
                puzzles.add(puzzle);
            } catch (InvalidPuzzleException | JSONException e) {
                Log.w(LOG_TAG, String.format(Locale.US,
                        "assets/%s is not a valid Tangram puzzle",
                        assetName), e);
            }
        }
        // To Do: Check for a translation file and
        // update the puzzle names if necessary
        initialized = true;
    }

    /**
     * @return {@code true} if the library has been initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @return the number of puzzles in the library
     */
    public int size() {
        return puzzles.size();
    }

    /**
     * Get a specific puzzle from the library
     *
     * @param index the index of the puzzle to retrieve
     *
     * @return the puzzle at the given index
     *
     * @throws IndexOutOfBoundsException if the index is negative
     * or &ge; {@link #size()}
     */
    public TangramPuzzle getPuzzle(int index) {
        return puzzles.get(index);
    }

    /**
     * Get a random puzzle from the library.
     *
     * @return a puzzle if any are available, or {@code null}
     * if the library is empty.
     */
    public TangramPuzzle getRandomPuzzle() {
        if (puzzles.isEmpty())
            return null;
        return puzzles.get((int) (Math.random() * puzzles.size()));
    }

}
