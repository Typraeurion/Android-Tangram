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
package com.xmission.trevin.android.tangram.exception;

import android.content.Context;

import androidx.annotation.NonNull;

import com.xmission.trevin.android.tangram.R;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Validation error for the case where a group of two or more pieces
 * of a Tangram is not touching another group of two or more pieces.
 * In the extreme case, there may be three such groups (with 7 pieces
 * total and 2&ndash;3 pieces in each group); in that case the validation
 * routine should create two of these errors.
 */
public class DisconnectedException extends TangramException {

    private final Set<Integer> group1Ids;
    private final Set<Integer> group2Ids;

    /**
     * Construct an exception for the given disconnected groups of pieces.
     *
     * @param group1Ids the resource IDs of the names of
     * the pieces in the first group
     * @param group1Names the names of the pieces in the first group
     * for internal logging
     * @param group2Ids the resource IDs of the names of
     * the pieces in the second group
     * @param group2Names the names of the pieces in the second group
     * for internal logging
     */
    public DisconnectedException(Set<Integer> group1Ids,
                                 Set<String> group1Names,
                                 Set<Integer> group2Ids,
                                 Set<String> group2Names) {
        super(String.format(Locale.US, "%s are not touching %s",
                group1Names, group2Names));
        this.group1Ids = Set.copyOf(group1Ids);
        this.group2Ids = Set.copyOf(group2Ids);
    }

    @Override
    public @NonNull String getMessage(Context context) {
        Set<String> group1Names = new TreeSet<>();
        Set<String> group2Names = new TreeSet<>();
        for (int id : group1Ids)
            group1Names.add(context.getString(id));
        for (int id : group2Ids)
            group2Names.add(context.getString(id));
        return context.getString(R.string.ValidationErrorDisconnect,
                group1Names, group2Names);
    }

}
