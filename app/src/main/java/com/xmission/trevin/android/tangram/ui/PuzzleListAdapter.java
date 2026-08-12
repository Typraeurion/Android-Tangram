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

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;

import com.xmission.trevin.android.tangram.R;
import com.xmission.trevin.android.tangram.data.PuzzleLibrary;
import com.xmission.trevin.android.tangram.data.TangramPuzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An adapter for displaying the puzzle library as a list.
 */
public class PuzzleListAdapter extends BaseAdapter {

    public static final String LOG_TAG = "PuzzleListAdapter";

    private final LayoutInflater inflater;

    private final PuzzleLibrary library;

    private final List<DataSetObserver> observers = new ArrayList<>();

    /**
     * Create the adapter with the given context.
     *
     * @param context the context in which the adapter is being used
     */
    public PuzzleListAdapter(@NonNull Context context) {
        Log.d(LOG_TAG, "Creating adapter");
        inflater = LayoutInflater.from(context);
        library = PuzzleLibrary.getInstance();
    }

    /** Indicate that all items in this adapter are enabled */
    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    /**
     * Get the number of items in the data set represented by this adapter
     *
     * @return the size of the library
     */
    @Override
    public int getCount() {
        return library.size();
    }

    /**
     * Get the puzzle associated with the specified position in the library.
     *
     * @param position the position in the list
     *
     * @return the puzzle for that position
     */
    @Override
    public TangramPuzzle getItem(int position) {
        if ((position < 0) || (position >= library.size())) {
            Log.w(LOG_TAG, String.format(Locale.US,
                    ".getItem(%d) - Invalid position", position));
            return null;
        }
        return library.getPuzzle(position);
    }

    /**
     * Get the row ID associated with the specified position in the list.
     * For our purposes this is the same as the position.
     *
     * @param position the position in the list
     *
     * @return the position
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Get a View that displays the puzzle at the specified position
     * in the library.
     *
     * @param position the position of the puzzle in the list
     * @param convertView the old view to reuse, if possible
     * @param parent the parent that this view will eventually be attached to
     *
     * @return a View of the puzzle at this position
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // For debug logging
        String cvDesc = (convertView == null) ? "null"
                : convertView.getClass().getSimpleName();
        if (convertView instanceof TextView)
            cvDesc = String.format("%s@%s(\"%s\")", cvDesc,
                    Integer.toHexString(System.identityHashCode(convertView)),
                    ((TextView) convertView).getText().toString());
        Log.d(LOG_TAG, String.format(Locale.US, ".getView(%d,%s,%s)",
                position, cvDesc, parent));
        TangramPuzzle puzzle = getItem(position);
        if (convertView instanceof ViewGroup vg) {
            TangramPuzzleView puzzleView = vg.findViewById(R.id.ListPuzzleView);
            TextView puzzleName = vg.findViewById(R.id.ListPuzzleName);
            if ((puzzleView != null) && (puzzleName != null)) {
                puzzleView.setPuzzle(puzzle);
                puzzleName.setText(puzzle.getName());
                return vg;
            }
        }
        Log.d(LOG_TAG, "Creating a new list item view");
        ViewGroup vg = (ViewGroup) inflater.inflate(
                R.layout.puzzle_list_item, parent, false);
        TangramPuzzleView puzzleView = vg.findViewById(R.id.ListPuzzleView);
        TextView puzzleName = vg.findViewById(R.id.ListPuzzleName);
        puzzleView.setPuzzle(puzzle);
        puzzleName.setText(puzzle.getName());
        return vg;
    }

    /**
     * ID&rsquo;s are not necessarily stable across changes to the
     * underlying data; in particular, the user-defined puzzles are
     * replaced entirely when the library (re-)loads user puzzles,
     * and the order in which they are read back is not guaranteed.
     *
     * @return {@code false}
     */
    @Override
    public boolean hasStableIds() {
        return false;
    }

    /**
     * @return {@code true} if the list contains no puzzles.
     */
    @Override
    public boolean isEmpty() {
        return library.size() == 0;
    }

    /**
     * All items for this list are always enabled.
     *
     * @param position (ignored)
     *
     * @return {@code true}
     */
    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    /**
     * Notify any attached observers that the underlying puzzles
     * have been changed and any View reflecting the data set
     * should refresh itself.
     */
    @Override
    public void notifyDataSetChanged() {
        Log.d(LOG_TAG, ".notifyDataSetChanged");
        for (DataSetObserver observer : observers) try {
            observer.onChanged();
        } catch (RuntimeException e) {
            Log.w(LOG_TAG, "Failed to notify observer "
                    + observer.getClass().getCanonicalName(), e);
        }
    }

    /**
     * Register an observer that is called when changes happen
     * to the puzzle library.
     *
     * @param observer the observer to notify when changes happen
     */
    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        observers.add(observer);
    }

    /**
     * Unregister an observer that has previously been registered with
     * {@link #registerDataSetObserver}
     *
     * @param observer the observer to unregister
     */
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        observers.remove(observer);
    }

}
