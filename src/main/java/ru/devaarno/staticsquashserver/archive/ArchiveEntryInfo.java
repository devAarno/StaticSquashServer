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

public final class ArchiveEntryInfo {
    private final String path;
    private final long size;
    private final MediaType mediaType;
    private final Instant modificationTime;

    public ArchiveEntryInfo(String path, long size, MediaType mediaType, Instant modificationTime) {
        this.path = path;
        this.size = size;
        this.mediaType = mediaType;
        this.modificationTime = modificationTime;
    }

    public String path() {
        return path;
    }

    public long size() {
        return size;
    }

    public MediaType mediaType() {
        return mediaType;
    }

    public Instant modificationTime() {
        return modificationTime;
    }
}
