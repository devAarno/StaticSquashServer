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

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Stage 8: Corrupted Archive Handling.
 * Tests that the application correctly handles corrupted archives.
 */
class CorruptedArchiveTest {

    @TempDir
    Path tempDir;

    @Test
    void testCorruptedZipArchive() {
        final var corruptedZip = tempDir.resolve("corrupted.zip");

        assertDoesNotThrow(() -> Files.write(corruptedZip, new byte[]{0x50, 0x4B, 0x03, 0x04, (byte) 0xFF, (byte) 0xFF}));
        
        final var parser = ArchiveParserFactory.create(corruptedZip);
        
        final var exception = assertThrows(
                RuntimeException.class,
                parser::initScan,
                "Should throw RuntimeException for corrupted ZIP"
        );
        assertTrue(
                exception.getMessage().contains("Failed to parse"),
                "Exception message should contain 'Failed to parse' for corrupted ZIP"
        );
    }

    @Test
    void testEmptyFileAsZip() {
        final var emptyFile = tempDir.resolve("empty.zip");

        assertDoesNotThrow(() -> Files.write(emptyFile, new byte[]{}));

        final var parser = ArchiveParserFactory.create(emptyFile);

        assertThrows(
                RuntimeException.class,
                parser::initScan,
                "Should throw RuntimeException for empty file as ZIP"
        );
    }

    @Test
    void testTruncatedZipArchive() {
        final var truncatedZip = tempDir.resolve("truncated.zip");
        byte[] partialZip = new byte[100];
        for (int i = 0; i < partialZip.length; i++) {
            partialZip[i] = (byte) (i & 0xFF);
        }

        assertDoesNotThrow(() -> Files.write(truncatedZip, partialZip));

        final var parser = ArchiveParserFactory.create(truncatedZip);

        assertThrows(
                RuntimeException.class,
                parser::initScan,
                "Should throw RuntimeException for truncated ZIP"
        );
    }

    @Test
    void testCorruptedTarGzArchive() {
        final var corruptedTarGz = tempDir.resolve("corrupted.tar.gz");

        assertDoesNotThrow(() -> Files.write(corruptedTarGz, new byte[]{(byte) 0x1F, (byte) 0x8B, (byte) 0x08, (byte) 0xFF, (byte) 0xFF}));

        final var parser = ArchiveParserFactory.create(corruptedTarGz);

        final var exception = assertThrows(
                RuntimeException.class,
                parser::initScan,
                "Should throw RuntimeException for corrupted TAR.GZ"
        );
        assertTrue(
                exception.getMessage().contains("Failed to parse"),
                "Exception message should contain 'Failed to parse' for corrupted TAR.GZ"
        );
    }

    @Test
    void testCorruptedTarXzArchive() {
        final var corruptedTarXz = tempDir.resolve("corrupted.tar.xz");

        assertDoesNotThrow(() -> Files.write(corruptedTarXz, new byte[]{(byte) 0xFD, (byte) 0x37, (byte) 0x7A, (byte) 0x58, (byte) 0x5A, (byte) 0x00, (byte) 0xFF}));

        final var parser = ArchiveParserFactory.create(corruptedTarXz);

        final var exception = assertThrows(
                RuntimeException.class,
                parser::initScan,
                "Should throw RuntimeException for corrupted TAR.XZ"
        );
        assertTrue(
                exception.getMessage().contains("Failed to parse"),
                "Exception message should contain 'Failed to parse' for corrupted TAR.XZ"
        );
    }

    @Test
    void testNonExistentArchive() {
        final var nonExistent = tempDir.resolve("nonexistent.zip");

        final var parser = ArchiveParserFactory.create(nonExistent);

        assertThrows(
                RuntimeException.class,
                parser::initScan,
                "Should throw RuntimeException for non-existent archive"
        );
    }

    @Test
    void testValidZipReturnsEntries() {
        final var validZip = tempDir.resolve("valid.zip");

        assertDoesNotThrow(
                () -> {
                    try (final var zipOutputStream = new java.util.zip.ZipOutputStream(Files.newOutputStream(validZip))) {
                        zipOutputStream.putNextEntry(new java.util.zip.ZipEntry("test.txt"));
                        zipOutputStream.write("Hello".getBytes());
                        zipOutputStream.closeEntry();
                    }
                }
        );
        
        final var parser = ArchiveParserFactory.create(validZip);
        final var descriptor = parser.initScan();
        
        assertEquals(1, descriptor.entries().size(), "Should have exactly 1 entry in valid ZIP");
        assertEquals("test.txt", descriptor.entries().getFirst().path(), "Entry final var should be 'test.txt'");
    }

    @Test
    void testArchiveWithOnlyDirectories() {
        final var zipWithDirs = tempDir.resolve("dirs_only.zip");

        assertDoesNotThrow(
                () -> {
                    try (final var zipOutputStream = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipWithDirs))) {
                        zipOutputStream.putNextEntry(new java.util.zip.ZipEntry("dir1/"));
                        zipOutputStream.closeEntry();
                        zipOutputStream.putNextEntry(new java.util.zip.ZipEntry("dir2/"));
                        zipOutputStream.closeEntry();
                    }
                }
        );

        
        final var parser = ArchiveParserFactory.create(zipWithDirs);
        final var descriptor = parser.initScan();
        
        assertTrue(
                descriptor.entries().isEmpty(),
                "Should have no file entries when archive contains only directories"
        );
    }
}
