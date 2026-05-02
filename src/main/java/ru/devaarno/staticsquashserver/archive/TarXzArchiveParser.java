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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Parser for TAR.XZ archive format.
 */
public final class TarXzArchiveParser implements ArchiveParser {
    
    @Override
    public ArchiveFormat format() {
        return ArchiveFormat.TAR_XZ;
    }
    
    @Override
    public ArchiveDescriptor parse(Path archivePath) {
        List<ArchiveEntryInfo> entries = new ArrayList<>();
        
        try (InputStream fis = Files.newInputStream(archivePath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             XZCompressorInputStream xzis = new XZCompressorInputStream(bis);
             TarArchiveInputStream tais = new TarArchiveInputStream(xzis)) {
            
            TarArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.add(new ArchiveEntryInfo(
                            entry.getName(),
                            entry.getSize(),
                            ArchiveParser.detectMediaType(entry.getName()),
                            Instant.ofEpochMilli(entry.getLastModifiedDate().getTime())
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse TAR.XZ archive: " + archivePath, e);
        }
        
        return new ArchiveDescriptor(archivePath, ArchiveFormat.TAR_XZ, entries);
    }
    
    @Override
    public InputStream getEntryInputStream(Path archivePath, String entryPath) {
        try {
            InputStream fis = Files.newInputStream(archivePath);
            BufferedInputStream bis = new BufferedInputStream(fis);
            XZCompressorInputStream xzis = new XZCompressorInputStream(bis);
            TarArchiveInputStream tais = new TarArchiveInputStream(xzis);
            
            TarArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                if (entry.getName().equals(entryPath)) {
                    return tais;
                }
            }
            
            tais.close();
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get entry stream from TAR.XZ archive: " + archivePath, e);
        }
    }
    
    @Override
    public void forEachEntry(Path archivePath, Consumer<ArchiveEntryInfo> entryConsumer) {
        try (InputStream fis = Files.newInputStream(archivePath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             XZCompressorInputStream xzis = new XZCompressorInputStream(bis);
             TarArchiveInputStream tais = new TarArchiveInputStream(xzis)) {
            
            TarArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entryConsumer.accept(new ArchiveEntryInfo(
                            entry.getName(),
                            entry.getSize(),
                            ArchiveParser.detectMediaType(entry.getName()),
                            Instant.ofEpochMilli(entry.getLastModifiedDate().getTime())
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to iterate TAR.XZ archive: " + archivePath, e);
        }
    }
}
