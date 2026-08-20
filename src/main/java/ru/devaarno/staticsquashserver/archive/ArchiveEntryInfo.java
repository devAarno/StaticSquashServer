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

import java.time.Instant;
import java.util.Objects;

public record ArchiveEntryInfo(
        String path,
        long size,
        MediaType mediaType,
        Instant modificationTime
) {

    public ArchiveEntryInfo {
        Objects.requireNonNull(path, "path must not be null");
    }

    /**
     * Canonical browser-requestable URL path derived from the raw archive {@link #path()}.
     */
    public String urlPath() {
        return ArchiveParser.canonicalizeUrlPath(path);
    }
}
