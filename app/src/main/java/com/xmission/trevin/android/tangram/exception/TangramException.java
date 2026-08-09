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

/**
 * Detail of a specific error when validating a Tangram puzzle.
 * This includes a hard-coded message for logging purposes
 * and references to string resources for displaying to the user.
 */
public abstract class TangramException extends IllegalArgumentException {

    /**
     * Constructs an {@code TangramException} with the specified
     * detail message.
     *
     * @param message the detail message
     */
    public TangramException(@NonNull String message) {
        super(message);
    }

    /**
     * Get a localized message for this exception from the Android resources.
     *
     * @param context the context in which to construct the message
     *
     * @return the formatted message
     */
    public abstract @NonNull String getMessage(Context context);

}
