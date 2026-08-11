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
import android.content.UriPermission;
import android.net.Uri;

public class FileUtils {

    /**
     * Check whether the app still has access to the user
     * puzzle directory.
     *
     * @param directoryUri the {@link Uri} of the directory
     *
     * @return {@code true} if the directory is still accessible,
     * {@code false} if not.
     */
    public static boolean isDirectoryAccessible(
            Context context, Uri directoryUri) {
        for (UriPermission permission : context.getContentResolver()
                .getPersistedUriPermissions()) {
            if (permission.getUri().equals(directoryUri) &&
                    permission.isWritePermission())
                return true;
        }
        return false;
    }

}
