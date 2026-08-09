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
 * Validation error for a missing piece (or a number of pieces of
 * a single type).
 */
public class MissingPieceException extends TangramException {

    private final int numMissing;
    private final int pieceId;

    /**
     * Construct an exception for a specific missing piece.
     *
     * @param n the number of this type of piece that are missing
     * @param pieceId the resource ID of the name of this piece
     * @param pieceName the name of this piece for internal logging
     */
    public MissingPieceException(int n, int pieceId, String pieceName) {
        super(String.format(Locale.US, (n == 1) ? "%d %s is missing"
                : "%d %ss are missing", n, pieceName));
        numMissing = n;
        this.pieceId = pieceId;
    }

    @Override
    public @NonNull String getMessage(Context context) {
        return context.getResources().getQuantityString(
                R.plurals.ValidationErrorMissingPiece,
                numMissing, numMissing, context.getString(pieceId));
    }

}
