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
import android.content.UriPermission;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.exception.InvalidPuzzleException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import kotlin.text.Charsets;

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

    private @Nullable List<TangramPuzzle> userPuzzles = null;

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
     * Read the contents of a JSON user file.
     *
     * @param context the context in which the application is running
     * @param fileRef the {@link DocumentFile} of the file to read
     *
     * @return the JSON array or object from the file,
     * or {@code null} if the file is empty.
     *
     * @throws IOException if the file cannot be read
     * @throws JSONException if the JSON is malformed, or if the content
     * is neither a {@link JSONArray} nor a {@link JSONObject}.
     */
    public @Nullable Object readJSONUserFile(
            @NonNull Context context, @NonNull DocumentFile fileRef)
        throws IOException, JSONException {
        StringBuilder builder = new StringBuilder();
        try (InputStream iStream = context.getContentResolver()
                .openInputStream(fileRef.getUri());
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     iStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line);
        }
        // Trim leading and trailing whitespace
        while (builder.length() > 0) {
            if (Character.isWhitespace(builder.charAt(builder.length() - 1)))
                builder.deleteCharAt(builder.length() - 1);
            else if (Character.isWhitespace(builder.charAt(0)))
                builder.deleteCharAt(0);
        }
        if (builder.length() == 0)
            return null;
        Object o = new JSONTokener(builder.toString()).nextValue();
        if (!(o instanceof JSONArray) && !(o instanceof JSONObject))
            throw new JSONException("Invalid JSON in " + fileRef.getName());
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
            }
            else if (json instanceof JSONObject jsonObject) try {
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
        Log.d(LOG_TAG, String.format(Locale.US,
                "%d puzzles loaded from assets", puzzles.size()));
        initialized = true;
    }

    /**
     * Load user-defined puzzles from the given folder.
     *
     * @param context the context in which the application is running
     * @param folder the folder containing the user puzzles
     *
     * @return a {@link List} of any errors encountered while reading
     * the puzzle files to be returned to the user, or {@code null}
     * if no errors were encountered.
     *
     * @throws IOException if there was an error reading the folder
     */
    public List<String> loadUserPuzzles(
            @NonNull Context context, @NonNull DocumentFile folder)
            throws IOException {
        List<String> errors = new ArrayList<>();
        // Check that the directory is readable
        boolean readable = false;
        for (UriPermission permission : context.getContentResolver()
                .getPersistedUriPermissions()) {
            if (permission.getUri().equals(folder.getUri()) &&
                    permission.isReadPermission()) {
                readable = true;
                break;
            }
        }
        if (!readable) {
            errors.add(context.getString(R.string.ErrorUserDirRevoked,
                    folder.getName()));
            return errors;
        }
        Pattern puzzleFilePattern = Pattern.compile(Pattern.quote(
                context.getString(R.string.UserPuzzleFilePrefix))
                + ".+\\.json", Pattern.CASE_INSENSITIVE);
        List<TangramPuzzle> newPuzzles = new ArrayList<>();
        for (DocumentFile file : folder.listFiles()) {
            if (file.getName() == null)
                continue;
            if (!puzzleFilePattern.matcher(file.getName()).matches())
                continue;
            String sourceFileName = file.getName();
            Object json = null;
            try {
                json = readJSONUserFile(context, file);
            } catch (IOException | JSONException e) {
                Log.w(LOG_TAG, String.format(Locale.US,
                        "Error reading user file %s; ignoring it.",
                        file.getName()), e);
                errors.add(context.getString(R.string.ErrorCannotReadUserFile,
                        file.getName(), e.getMessage()));
                continue;
            }
            if (json instanceof JSONArray jsonArray) {
                for (int i = 0; i < jsonArray.length(); i++) try {
                    TangramPuzzle puzzle = new TangramPuzzle(
                            jsonArray.getJSONObject(i));
                    puzzle.setSourceFileName(sourceFileName);
                    puzzles.add(puzzle);
                } catch (InvalidPuzzleException | JSONException e) {
                    Log.w(LOG_TAG, String.format(Locale.US,
                            "Entry at %s[%d] is not a valid Tangram puzzle",
                            file.getName(), i), e);
                }
            }
            else if (json instanceof JSONObject jsonObject) try {
                TangramPuzzle puzzle = new TangramPuzzle(jsonObject);
                puzzle.setSourceFileName(sourceFileName);
                puzzles.add(puzzle);
            } catch (InvalidPuzzleException | JSONException e) {
                Log.w(LOG_TAG, String.format(Locale.US,
                        "%s is not a valid Tangram puzzle",
                        file.getName()), e);
            }
        }
        if (newPuzzles.isEmpty()) {
            Log.d(LOG_TAG, String.format(Locale.US,
                    "No %s puzzles found in %s",
                    errors.isEmpty() ? "user" : "valid", folder.getName()));
        } else {
            if (userPuzzles != null)
                userPuzzles.clear();
            userPuzzles = newPuzzles;
            Log.d(LOG_TAG, String.format(Locale.US,
                    "%d user puzzles loaded from %s",
                    newPuzzles.size(), folder.getName()));
        }
        return errors.isEmpty() ? null : errors;
    }

    /**
     * Save a new user puzzle to the given file.  If the file already
     * exists, this will read the file first and check for a puzzle
     * with the same ID, replacing it if found otherwise adding the
     * given puzzle to the JSON array.  This also adds (or replaces)
     * the puzzle in the library.
     *
     * @param context the context in which the application is running
     * @param puzzle the puzzle to save
     * @param saveFile the file to save the puzzle to
     *
     * @throws IOException if there was an error reading or writing the file
     * @throws JSONException if there was an error parsing the JSON
     */
    public void savePuzzle(@NonNull Context context,
                           @NonNull TangramPuzzle puzzle,
                           @NonNull DocumentFile saveFile)
            throws IOException, JSONException {
        // Ensure the puzzle has an ID.
        if (puzzle.getId() == null)
            puzzle.setId(UUID.randomUUID().toString());
        // Start by updating the library; this should succeed
        // regardless of whether updating the file is successful.
        if (userPuzzles == null) {
            userPuzzles = new ArrayList<>();
        } else {
            for (TangramPuzzle existing : userPuzzles) {
                if (existing.getId().equals(puzzle.getId())) {
                    userPuzzles.remove(existing);
                    break;
                }
            }
        }
        userPuzzles.add(puzzle);
        Object json = null;
        if (saveFile.exists()) {
            json = readJSONUserFile(context, saveFile);
            if (json instanceof JSONArray jsonArray) {
                // Scan for a matching ID without fully parsing the puzzles
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject element = jsonArray.getJSONObject(i);
                    if (!element.has(TangramPuzzle.JSON_ID))
                        continue;
                    if (element.getString(TangramPuzzle.JSON_ID)
                            .equals(puzzle.getId())) {
                        jsonArray.remove(i);
                        break;
                    }
                }
            }
            else if (json instanceof JSONObject jsonObject) {
                if (!jsonObject.has(TangramPuzzle.JSON_ID) ||
                        !jsonObject.getString(TangramPuzzle.JSON_ID)
                                .equals(puzzle.getId())) {
                    // Make this an array
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(jsonObject);
                    json = jsonArray;
                } else {
                    json = null;
                }
            }
        }
        JSONObject puzzleJson = puzzle.toJSON();
        String jsonString;
        if (json instanceof JSONArray jsonArray) {
            jsonArray.put(puzzleJson);
            jsonString = jsonArray.toString(2);
        } else {
            jsonString = puzzleJson.toString(2);
        }
        try (ParcelFileDescriptor pfd = context.getContentResolver()
                .openFileDescriptor(saveFile.getUri(), "wt")) {
            if (pfd == null) {
                throw new IOException(context.getString(
                        R.string.ErrorCannotCreateSaveFile,
                        saveFile.getName()));
            }
            try (FileOutputStream fos = new FileOutputStream(
                    pfd.getFileDescriptor());
                 OutputStreamWriter writer = new OutputStreamWriter(
                         fos, Charsets.UTF_8)) {
                writer.write(jsonString);
                writer.flush();
            }
        }
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
        return puzzles.size() + ((userPuzzles == null)
                ? 0 : userPuzzles.size());
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
        if (index < puzzles.size())
            return puzzles.get(index);
        if (userPuzzles == null)
            throw new IndexOutOfBoundsException(String.format(Locale.US,
                    "index=%d, size=%d", index, puzzles.size()));
        return userPuzzles.get(index - puzzles.size());
    }

    /**
     * Get a random puzzle from the library.
     *
     * @return a puzzle if any are available, or {@code null}
     * if the library is empty.
     */
    public TangramPuzzle getRandomPuzzle() {
        int max = size();
        if (max == 0)
            return null;
        return getPuzzle((int) (Math.random() * max));
    }

}
