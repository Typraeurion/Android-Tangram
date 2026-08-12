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
package com.xmission.trevin.android.tangram.util;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Helpers that centralize the boilerplate for reading and writing text
 * files, shared by the puzzle loader and saver.
 *
 * @author Claude Opus 4.8
 */
public final class FileUtils {

    private static final int BUFFER_SIZE = 4096;

    private FileUtils() {
    }

    /**
     * Derive a human-friendly folder name from a Storage Access Framework
     * tree {@link Uri}.  Unlike {@code DocumentFile.getName()}, this parses
     * the URI's document ID rather than querying the provider, so it still
     * works when access has been revoked (when {@code getName()} returns
     * {@code null}).
     *
     * <p>External-storage document IDs look like
     * {@code "primary:Documents/Tangram"}; the final path component
     * ({@code "Tangram"}) is returned.</p>
     *
     * @param treeUri the tree (or tree-document) URI
     *
     * @return a best-effort display name, falling back to the raw URI
     * string if none can be derived
     */
    public static @NonNull String getFriendlyName(@NonNull Uri treeUri) {
        String id;
        try {
            id = DocumentsContract.getTreeDocumentId(treeUri);
        } catch (IllegalArgumentException notATreeUri) {
            id = treeUri.getLastPathSegment();
        }
        if (id == null)
            return treeUri.toString();
        // Reduce e.g. "primary:Documents/Tangram" to its last component.
        int cut = Math.max(id.lastIndexOf('/'), id.lastIndexOf(':'));
        String name = (cut >= 0) ? id.substring(cut + 1) : id;
        return name.isEmpty() ? treeUri.toString() : name;
    }

    /**
     * Read an entire stream as UTF-8 text.  The caller retains ownership of
     * the stream and is responsible for closing it (e.g. with
     * try-with-resources).
     *
     * @param in the stream to read
     *
     * @return the stream's contents as a string
     *
     * @throws IOException if the stream cannot be read
     */
    public static @NonNull String readText(@NonNull InputStream in)
            throws IOException {
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[BUFFER_SIZE];
        int count;
        while ((count = reader.read(buffer)) >= 0)
            builder.append(buffer, 0, count);
        return builder.toString();
    }

    /**
     * Write UTF-8 text to a document, truncating any existing content.
     *
     * @param context the context in which the application is running
     * @param uri the document {@link Uri} to write to
     * @param text the text to write
     *
     * @throws IOException if the document cannot be opened or written
     */
    public static void writeText(@NonNull Context context, @NonNull Uri uri,
                                 @NonNull String text) throws IOException {
        try (ParcelFileDescriptor pfd = context.getContentResolver()
                .openFileDescriptor(uri, "wt")) {
            if (pfd == null)
                throw new IOException("Could not open " + uri + " for writing");
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(pfd.getFileDescriptor()),
                    StandardCharsets.UTF_8)) {
                writer.write(text);
                writer.flush();
            }
        }
    }

}
