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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown and container for details when parsing a JSON
 * puzzle file or validating a user-built puzzle which does not
 * contain the exact set of pieces needed in a Tangram
 * (two small triangles, one square, one parallelogram, one
 * medium triangle, and two large triangles), where pieces overlap,
 * and/or where pieces are not all connected.
 *
 * @author Trevin Beattie
 */
public class InvalidPuzzleException extends IllegalArgumentException {

    /** Resource ID of the exception message, or -1 if it has none */
    private final int messageId;
    /** Name of the puzzle that caused this exception, if any */
    private final String puzzleName;

    /** List of validation errors */
    private final List<TangramException> validationErrors = new ArrayList<>();

    /**
     * Constructs an {@code InvalidPuzzleException} with no detail
     * message.
     */
    public InvalidPuzzleException() {
        super();
        messageId = -1;
        puzzleName = null;
    }

    /**
     * Constructs an {@code InvalidPuzzleException} with the specified
     * detail message.
     *
     * @param message the detail message
     */
    public InvalidPuzzleException(String message) {
        super(message);
        messageId = -1;
        puzzleName = null;
    }

    /**
     * Constructs an {@code InvalidPuzzleException} with the specified
     * detail message and message resource.
     *
     * @param message the detail message
     * @param id the ID of a string resource for the localized message
     */
    public InvalidPuzzleException(String message, int id) {
        super(message);
        messageId = id;
        puzzleName = null;
    }

    /**
     * Constructs an {@code InvalidPuzzleException} with the specified
     * cause and a detail message copied from the cause, if any.
     *
     * @param cause the cause (which is saved for later retrieval by
     *              the {@link #getCause()} method).  (A {@code null}
     *              value is permitted, and indicates that the cause is
     *              nonexistent or unknown.)
     */
    public InvalidPuzzleException(Throwable cause) {
        super(cause);
        messageId = -1;
        puzzleName = null;
        if (cause instanceof TangramException te)
            validationErrors.add(te);
    }

    /**
     * Constructs an {@code InvalidPuzzleException} with the specified
     * detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause (which is saved for later retrieval by
     *              the {@link #getCause()} method).  (A {@code null}
     *              value is permitted, and indicates that the cause is
     *              nonexistent or unknown.)
     */
    public InvalidPuzzleException(String message, Throwable cause) {
        super(message, cause);
        messageId = -1;
        puzzleName = null;
        if (cause instanceof TangramException te)
            validationErrors.add(te);
    }

    /**
     * Constructs an {@code InvalidPuzzleException} with the specified
     * detail message, message resource, and cause.
     *
     * @param message the detail message
     * @param id the ID of a string resource for the localized message
     * @param cause the cause (which is saved for later retrieval by
     *              the {@link #getCause()} method).  (A {@code null}
     *              value is permitted, and indicates that the cause is
     *              nonexistent or unknown.)
     */
    public InvalidPuzzleException(String message, int id, Throwable cause) {
        super(message, cause);
        messageId = id;
        puzzleName = null;
        if (cause instanceof TangramException te)
            validationErrors.add(te);
    }

    /**
     * Constructs an {@code InvalidPuzzleException} with the specified
     * validation errors and no wrapper message.
     *
     * @param errors the validation errors
     */
    public InvalidPuzzleException(List<TangramException> errors) {
        super();
        messageId = -1;
        puzzleName = null;
        validationErrors.addAll(errors);
    }

    /**
     * Constructs an {@code InvalidPuzzleException} with the specified
     * detail message and validation errors.
     *
     * @param message the detail message
     * @param id the ID of a string resource for the localized message
     * @param name the name of the puzzle that has these errors
     * @param errors the validation errors
     */
    public InvalidPuzzleException(String message, int id, String name,
                                  List<TangramException> errors) {
        super(message);
        messageId = id;
        puzzleName = name;
        validationErrors.addAll(errors);
    }

    /**
     * Add a validation error to this exception.
     *
     * @param error the error to add
     */
    public void addValidationError(TangramException error) {
        validationErrors.add(error);
    }

    /**
     * @return the list of validation errors
     */
    public List<TangramException> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors);
    }

    /**
     * Get the detail message for this exception, including the
     * messages of all the validation errors.  This returns the
     * non-localized message strings.
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        String overview = super.getMessage();
        if (overview != null)
            sb.append(overview).append('\n');
        for (TangramException error : validationErrors) {
            String message = error.getMessage();
            if ((message == null) || message.isEmpty())
                message = error.getClass().getSimpleName();
            sb.append(message).append('\n');
        }
        return (sb.length() <= 0) ? null : sb.toString();
    }

    /**
     * Get the detail message for this exception, including the
     * messages of all the validation errors.  This returns
     * localized message strings from the Android resources.
     *
     * @param context the context in which to construct the message
     *
     * @return the formatted message
     */
    public @NonNull String getMessage(Context context) {
        StringBuilder sb = new StringBuilder();
        String overview = context.getString(messageId, puzzleName);
        sb.append(overview).append('\n');
        for (TangramException error : validationErrors) {
            String message = error.getMessage(context);
            sb.append(message).append('\n');
        }
        return (sb.length() <= 0) ? "" : sb.toString();
    }

}
