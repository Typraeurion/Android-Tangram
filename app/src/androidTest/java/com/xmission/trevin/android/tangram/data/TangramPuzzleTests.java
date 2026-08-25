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
import java.util.Locale;

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

    Context testContext = null;

    /**
     * Get the context in which the tests are running,
     * if not already set.
     */
    @Before
    public void getTestContext() {
        if (testContext == null)
            testContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext();
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
        try (InputStream iStream = testContext.getAssets().open(assetName);
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
     * Run the test for a given file.
     *
     * @param assetName the name of the puzzle asset to read
     *
     * @throws IOException if the asset does not exist or cannot be read.
     * @throws JSONException if the asset is not a valid JSON array.
     */
    private void runTest(String assetName) throws IOException, JSONException {
        String jsonString = readAsset(assetName);
        JSONArray json = new JSONArray(jsonString);
        assertNotEquals(String.format(Locale.US, "No puzzles found in %s",
                        assetName), 0, json.length());
        for (int i = 0; i < json.length(); i++) {
            TangramPuzzle puzzle = new TangramPuzzle(json.getJSONObject(i));
            /*
             * No further testing at this time; the constructor
             * already checks whether the puzzle is valid.
             */
        }
    }

    /**
     * Test the base puzzles file.  This one includes the square
     * that Tangram pieces are made from, plus a group of puzzles
     * copied from (or based on) ones found on www.myhomeschoolmath.com.
     */
    @Test
    public void testReadBasePuzzles() throws IOException, JSONException {
        runTest("puzzles-01-base.json");
    }

    /**
     * Test the geometric puzzles file.  This one includes a group of puzzles
     * copied from (or based on) ones found on www.myhomeschoolmath.com.
     */
    @Test
    public void testReadGeometricPuzzles() throws IOException, JSONException {
        runTest("puzzles-02-geometric.json");
    }

    /**
     * Test the objects puzzles file.  This one includes a group of puzzles
     * copied from (or based on) ones found on www.myhomeschoolmath.com.
     */
    @Test
    public void testReadObjectPuzzles() throws IOException, JSONException {
        runTest("puzzles-06-objects.json");
    }

    /**
     * Test the animal puzzles file.  This one includes a group of
     * puzzles copied from ones found on www.myhomeschoolmath.com,
     * with a few name changes.
     */
    @Test
    public void testReadAnimalPuzzles() throws IOException, JSONException {
        runTest("puzzles-10-animals.json");
    }

    /**
     * Test the people puzzles file.  This one includes a group of
     * puzzles copied from ones found on www.myhomeschoolmath.com,
     * with a few name changes.
     */
    @Test
    public void testReadPeoplePuzzles() throws IOException, JSONException {
        runTest("puzzles-14-people.json");
    }

    /**
     * Test the Christmas puzzles file.  This one includes a group of
     * puzzles copied from ones found on www.myhomeschoolmath.com.
     */
    @Test
    public void testReadChristmasPuzzles() throws IOException, JSONException {
        String jsonString = readAsset("puzzles-18-Christmas.json");
    }

    /**
     * Test the number puzzles file.  This one includes a group of
     * puzzles copied from ones found on www.myhomeschoolmath.com.
     */
    @Test
    public void testReadNumberPuzzles() throws IOException, JSONException {
        runTest("puzzles-22-numbers.json");
    }

    /**
     * Test the alphabet puzzles file.  This one includes most of the
     * letters found on Shutterstock image 2270175397 and a few
     * substitutions from various other sources.
     */
    @Test
    public void testReadAlphabetPuzzles() throws IOException, JSONException {
        runTest("puzzles-26-alphabet.json");
    }

}
