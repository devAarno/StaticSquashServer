/*
 * Static Squash Server --- a HTTP server for archived content
 *
 * Copyright (C) 2026 Parkhomenko Stanislav
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.devaarno.staticsquashserver.archive;

import io.helidon.common.media.type.MediaType;
import ru.devaarno.staticsquashserver.mime.MimeTypeResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public interface ArchiveParser {
    

    ArchiveDescriptor initScan();

    void fillOutput(final String entryPath, final OutputStream outputStream) throws IOException;

    ArchiveFormat format();

    static MediaType detectMediaType(final String entryName) {
        return MimeTypeResolver.resolve(entryName);
    }

    /**
     * Produces a browser-requestable URL path (RFC 3986 dot-segment removal) from a raw
     * archive entry name. Handles Windows {@code \} separators, leading {@code /} and
     * {@code ./} prefixes, duplicate slashes and {@code ..} segments without escaping the
     * archive root.
     */
    static String canonicalizeUrlPath(final String entryName) {
        String p = entryName.replace('\\', '/');
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        final String normalized = Path.of(p).normalize().toString().replace('\\', '/');
        return normalized.equals(".") ? "" : normalized;
    }

    void forEachEntry(final BiConsumer<String, InputStream> action) throws IOException;
}
