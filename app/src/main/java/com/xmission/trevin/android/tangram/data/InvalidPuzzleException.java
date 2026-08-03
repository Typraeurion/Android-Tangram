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

/**
 * Exception thrown when parsing a JSON puzzle file which does not
 * contain the exact set of pieces needed in a Tangram
 * (two small triangles, one square, one parallelogram, one
 * medium triangle, and two large triangles).
 *
 * @author Trevin Beattie
 */
public class InvalidPuzzleException extends IllegalArgumentException {

    /**
     * Constructs an {@code InvalidPuzzleException} with no detail
     * message.
     */
    public InvalidPuzzleException() {
        super();
    }

    /**
     * Constructs an {@code InvalidPuzzleException} with the specified
     * detail message.
     *
     * @param message the detail message
     */
    public InvalidPuzzleException(String message) {
        super(message);
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
    }

}
