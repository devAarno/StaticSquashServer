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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Stage 8: Corrupted Archive Handling.
 * Tests that the application correctly handles corrupted archives.
 */
class CorruptedArchiveTest {

    @TempDir
    Path tempDir;

    @Test
    void testCorruptedZipArchive() throws IOException {
        Path corruptedZip = tempDir.resolve("corrupted.zip");
        Files.write(corruptedZip, new byte[]{0x50, 0x4B, 0x03, 0x04, (byte) 0xFF, (byte) 0xFF});
        
        ArchiveParser parser = ArchiveParserFactory.create(corruptedZip);
        
        assertThatThrownBy(() -> parser.parse(corruptedZip))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse");
    }

    @Test
    void testEmptyFileAsZip() throws IOException {
        Path emptyFile = tempDir.resolve("empty.zip");
        Files.write(emptyFile, new byte[]{});
        
        ArchiveParser parser = ArchiveParserFactory.create(emptyFile);
        
        assertThatThrownBy(() -> parser.parse(emptyFile))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testTruncatedZipArchive() throws IOException {
        Path truncatedZip = tempDir.resolve("truncated.zip");
        byte[] partialZip = new byte[100];
        for (int i = 0; i < partialZip.length; i++) {
            partialZip[i] = (byte) (i & 0xFF);
        }
        Files.write(truncatedZip, partialZip);
        
        ArchiveParser parser = ArchiveParserFactory.create(truncatedZip);
        
        assertThatThrownBy(() -> parser.parse(truncatedZip))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testCorruptedTarGzArchive() throws IOException {
        Path corruptedTarGz = tempDir.resolve("corrupted.tar.gz");
        Files.write(corruptedTarGz, new byte[]{(byte) 0x1F, (byte) 0x8B, (byte) 0x08, (byte) 0xFF, (byte) 0xFF});
        
        ArchiveParser parser = ArchiveParserFactory.create(corruptedTarGz);
        
        assertThatThrownBy(() -> parser.parse(corruptedTarGz))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse");
    }

    @Test
    void testCorruptedTarXzArchive() throws IOException {
        Path corruptedTarXz = tempDir.resolve("corrupted.tar.xz");
        Files.write(corruptedTarXz, new byte[]{(byte) 0xFD, (byte) 0x37, (byte) 0x7A, (byte) 0x58, (byte) 0x5A, (byte) 0x00, (byte) 0xFF});
        
        ArchiveParser parser = ArchiveParserFactory.create(corruptedTarXz);
        
        assertThatThrownBy(() -> parser.parse(corruptedTarXz))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse");
    }

    @Test
    void testNonExistentArchive() {
        Path nonExistent = tempDir.resolve("nonexistent.zip");
        
        ArchiveParser parser = ArchiveParserFactory.create(nonExistent);
        
        assertThatThrownBy(() -> parser.parse(nonExistent))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testValidZipReturnsEntries() throws IOException {
        Path validZip = tempDir.resolve("valid.zip");
        
        try (var zipOutputStream = new java.util.zip.ZipOutputStream(Files.newOutputStream(validZip))) {
            zipOutputStream.putNextEntry(new java.util.zip.ZipEntry("test.txt"));
            zipOutputStream.write("Hello".getBytes());
            zipOutputStream.closeEntry();
        }
        
        ArchiveParser parser = ArchiveParserFactory.create(validZip);
        ArchiveDescriptor descriptor = parser.parse(validZip);
        
        assertThat(descriptor.entries()).hasSize(1);
        assertThat(descriptor.entries().get(0).path()).isEqualTo("test.txt");
    }

    @Test
    void testArchiveWithOnlyDirectories() throws IOException {
        Path zipWithDirs = tempDir.resolve("dirs_only.zip");
        
        try (var zipOutputStream = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipWithDirs))) {
            zipOutputStream.putNextEntry(new java.util.zip.ZipEntry("dir1/"));
            zipOutputStream.closeEntry();
            zipOutputStream.putNextEntry(new java.util.zip.ZipEntry("dir2/"));
            zipOutputStream.closeEntry();
        }
        
        ArchiveParser parser = ArchiveParserFactory.create(zipWithDirs);
        ArchiveDescriptor descriptor = parser.parse(zipWithDirs);
        
        assertThat(descriptor.entries()).isEmpty();
    }
}
