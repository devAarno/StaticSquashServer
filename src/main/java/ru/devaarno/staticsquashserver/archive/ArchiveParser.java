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

import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Interface for parsing archive files and extracting entry streams.
 */
public interface ArchiveParser {
    
    /**
     * Parses the archive and returns a descriptor with all file entries.
     * 
     * @param archivePath path to the archive file
     * @return archive descriptor containing metadata about all entries
     * @throws RuntimeException if archive is corrupted or cannot be parsed
     */
    ArchiveDescriptor parse(Path archivePath);
    
    /**
     * Gets an input stream for a specific entry in the archive.
     * The caller is responsible for closing the returned stream.
     * 
     * @param archivePath path to the archive file
     * @param entryPath path of the entry within the archive
     * @return input stream for the entry, or null if entry not found
     * @throws RuntimeException if archive is corrupted or cannot be read
     */
    InputStream getEntryInputStream(final Path archivePath, final String entryPath);
    
    /**
     * Returns the format this parser handles.
     * 
     * @return the archive format
     */
    ArchiveFormat format();
    
    /**
     * Helper method to detect media type from entry name.
     * 
     * @param entryName the name of the entry
     * @return the detected media type
     */
    static MediaType detectMediaType(final String entryName) {
        return MimeTypeResolver.resolve(entryName);
    }
    
    /**
     * Helper method to collect all non-directory entries from an archive.
     * 
     * @param archivePath path to the archive
     * @param entryConsumer consumer that processes each entry
     * @throws RuntimeException if archive is corrupted
     */
    void forEachEntry(final Path archivePath, final Consumer<ArchiveEntryInfo> entryConsumer);
}
