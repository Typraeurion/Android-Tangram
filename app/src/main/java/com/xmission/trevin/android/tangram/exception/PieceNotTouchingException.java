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
 * Validation error far a piece which is not touching any other
 * part of the Tangram.
 */
public class PieceNotTouchingException extends TangramException {

    private final int pieceId;

    /**
     * Construct an exception for a specific piece.
     *
     * @param pieceId the resource ID of the name of this piece
     * @param pieceName the name of this piece for internal logging
     */
    public PieceNotTouchingException(int pieceId, String pieceName) {
        super(String.format(Locale.US,
                "%s does not touch any other pieces", pieceName));
        this.pieceId = pieceId;
    }

    @Override
    public @NonNull String getMessage(Context context) {
        return context.getString(R.string.ValidationErrorNotTouching,
                context.getString(pieceId));
    }

}
