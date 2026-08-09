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

import java.util.Locale;

/**
 * Validation error for a piece which overlaps another piece
 * in a Tangram.
 */
public class OverlappingPiecesException extends TangramException {

    private final int[] pieceIds;

    /**
     * Construct an exception for the given overlapping pieces.
     *
     * @param piece1Id the resource ID of the name of the first piece
     * @param piece1Name the name of the first piece for internal logging
     * @param piece2Id the resource ID of the name of the second piece
     * @param piece2Name the name of the second piece for internal logging
     */
    public OverlappingPiecesException(int piece1Id, String piece1Name,
                                       int piece2Id, String piece2Name) {
        super(String.format(Locale.US,
                "%s overlaps %s", piece1Name, piece2Name));
        pieceIds = new int[] { piece1Id, piece2Id };
    }

    @Override
    public @NonNull String getMessage(Context context) {
        return context.getString(R.string.ValidationErrorOverlap,
                context.getString(pieceIds[0]),
                context.getString(pieceIds[1]));
    }

}
