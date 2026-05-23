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
import org.apache.commons.compress.compressors.CompressorInputStream;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


abstract class AbstractTarArchiveParser implements ArchiveParser {

    private final Path actualArchivePath;

    protected AbstractTarArchiveParser(final Path actualArchivePath) {
        this.actualArchivePath = actualArchivePath;
    }

    public abstract CompressorInputStream getCompressionChainMethod(final InputStream inputStream) throws IOException;
    
    @Override
    public ArchiveDescriptor initScan() {
        List<ArchiveEntryInfo> entries = new ArrayList<>();
        
        try (
                final var fis = Files.newInputStream(actualArchivePath);
                final var bis = new BufferedInputStream(fis);
                final var cis = getCompressionChainMethod(bis);
                final var tais = new TarArchiveInputStream(cis)
        ) {
            
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
        } catch (final Exception e) {
            throw new RuntimeException("Failed to parse " + format() + " archive: " + actualArchivePath, e);
        }
        
        return new ArchiveDescriptor(actualArchivePath, format(), entries);
    }
    
    @Override
    public void fillOutput(final String entryPath, final OutputStream outputStream) throws IOException {
        try (
                final var fis = Files.newInputStream(actualArchivePath);
                final var bis = new BufferedInputStream(fis);
                final var cis = getCompressionChainMethod(bis);
                final var tais = new TarArchiveInputStream(cis)
        ) {
            TarArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                if (entry.getName().equals(entryPath)) {
                    tais.transferTo(outputStream);
                    return;
                }
            }
        }
        throw new FileNotFoundException();
    }
}
