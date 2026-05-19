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

import java.nio.file.Path;
import java.util.Locale;


public final class ArchiveParserFactory {
    
    private ArchiveParserFactory() { }

    public static ArchiveParser create(final Path archivePath) {
        String name = archivePath.getFileName().toString().toLowerCase(Locale.getDefault());
        
        if (name.endsWith(".zip")) {
            return new ZipArchiveParser(archivePath);
        }
        if (name.endsWith(".tar.gz")) {
            return new TarGzArchiveParser(archivePath);
        }
        if (name.endsWith(".tar.xz")) {
            return new TarXzArchiveParser(archivePath);
        }
        
        throw new IllegalArgumentException("Unsupported archive format: " + archivePath.getFileName());
    }
}
