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

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that we're able to parse the puzzle asset files,
 * and that all puzzles are valid.  Validation is automatically
 * done during conversion from JSON objects.
 *
 * @author Trevin Beattie
 */
public class TangramPuzzleTests {

    Context hostContext = null;

    /**
     * Get the context in which the tests are running,
     * if not already set.
     */
    @Before
    public void getHostContext() {
        if (hostContext == null)
            hostContext = InstrumentationRegistry.getInstrumentation()
                    .getContext();
    }

    /**
     * Read the contents of an asset file
     * (normally packaged with the APK).
     *
     * @param assetName the name of the file in the
     * &ldquo;assets/&rdquo; folder
     *
     * @return the file contents as a String
     *
     * @throws IOException if the asset does not exist or cannot be read.
     */
    public String readAsset(String assetName) throws IOException {
        try (InputStream iStream = hostContext.getAssets().open(assetName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     iStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line);
            return builder.toString();
        }
    }

    /**
     * Test the basic puzzles file.  This one includes the square
     * that Tangram pieces are made from, plus a group of puzzles
     * copied from (or based on) ones found on www.myhomeschoolmath.com.
     */
    @Test
    public void testReadBasicPuzzles() throws IOException, JSONException {
        String jsonString = readAsset("puzzles-basic.json");
        JSONArray json = new JSONArray(jsonString);
        assertNotEquals("No puzzles found in puzzles-basic.json",
                0, json.length());
        for (int i = 0; i < json.length(); i++) {
            TangramPuzzle puzzle = new TangramPuzzle(json.getJSONObject(i));
            /*
             * No further testing at this time; the constructor
             * already checks whether the puzzle is valid.
             */
        }
    }

    /**
     * Test the alphabet puzzles file.  This one includes the square
     * that Tangram pieces are made from, plus a group of puzzles
     * copied from (or based on) ones found on www.myhomeschoolmath.com.
     */
    @Test
    public void testReadAlphabetPuzzles() throws IOException, JSONException {
        String jsonString = readAsset("puzzles-alphabet.json");
        JSONArray json = new JSONArray(jsonString);
        assertNotEquals("No puzzles found in puzzles-alphabet.json",
                0, json.length());
        for (int i = 0; i < json.length(); i++) {
            TangramPuzzle puzzle = new TangramPuzzle(json.getJSONObject(i));
        }
    }

    /**
     * Test the animal puzzles file.  This one includes the square
     * that Tangram pieces are made from, plus a group of puzzles
     * copied from (or based on) ones found on www.myhomeschoolmath.com.
     */
    @Test
    public void testReadAnimalPuzzles() throws IOException, JSONException {
        String jsonString = readAsset("puzzles-animals.json");
        JSONArray json = new JSONArray(jsonString);
        assertNotEquals("No puzzles found in puzzles-animals.json",
                0, json.length());
        for (int i = 0; i < json.length(); i++) {
            TangramPuzzle puzzle = new TangramPuzzle(json.getJSONObject(i));
        }
    }

}
