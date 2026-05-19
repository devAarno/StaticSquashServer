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

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Parser for ZIP archive format.
 */
public final class ZipArchiveParser implements ArchiveParser {

    private final Path actualArchivePath;

    public ZipArchiveParser(final Path archivePath) {
        this.actualArchivePath = archivePath;
    }

    @Override
    public ArchiveFormat format() {
        return ArchiveFormat.ZIP;
    }
    
    @Override
    public ArchiveDescriptor initScan() {
        List<ArchiveEntryInfo> entries = new ArrayList<>();
        
        try (ZipFile zipFile = ZipFile.builder().setPath(actualArchivePath).get()) {
            Enumeration<ZipArchiveEntry> entriesEnum = zipFile.getEntries();
            
            while (entriesEnum.hasMoreElements()) {
                ZipArchiveEntry entry = entriesEnum.nextElement();
                
                if (!entry.isDirectory()) {
                    entries.add(new ArchiveEntryInfo(
                            entry.getName(),
                            entry.getSize(),
                            ArchiveParser.detectMediaType(entry.getName()),
                            Instant.ofEpochMilli(entry.getTime())
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ZIP archive: " + actualArchivePath, e);
        }
        
        return new ArchiveDescriptor(actualArchivePath, ArchiveFormat.ZIP, entries);
    }
    
    @Override
    public void fillOutput(final String entryPath, final OutputStream outputStream) throws IOException {
        try (final var zipFile = ZipFile.builder().setPath(actualArchivePath).get()) {
            ZipArchiveEntry entry = zipFile.getEntry(entryPath);
            
            if (entry == null) {
                throw new FileNotFoundException();
            }
            
            zipFile.getInputStream(entry).transferTo(outputStream);
        }
    }
}
