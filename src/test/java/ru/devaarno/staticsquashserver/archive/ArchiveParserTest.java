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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ArchiveParser implementations.
 * Tests edge cases in tree structure handling.
 */
class ArchiveParserTest {

    private static final URI ZIP_FILE_URI = assertDoesNotThrow(
            () -> Objects.requireNonNull(
                    ArchiveParserTest.class.getResource("/test_archives/test.zip")
            ).toURI()
    );
    private static final long SPACES_TXT_CRC32 = 832580676L;
    private static final long LARGE_DUMMY_BIN_CRC32 = 2694514304L;
    private static final long LEVEL3_JSON_CRC32 = 4118783144L;
    private static final String CHECKSUM_MISMATCH = "CRC32 checksum mismatch";

    @Test
    void testZipParsing() throws IOException {
        try (final var zipFile = ZipFile.builder().setURI(ZIP_FILE_URI).get()) {
            final Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            int fileCount = 0;

            while (entries.hasMoreElements()) {
                final ZipArchiveEntry entry = entries.nextElement();
                assertFalse(entry.getName().startsWith("/"),
                        "Entry name should not start with /: " + entry.getName());

                if (!entry.isDirectory()) {
                    fileCount++;
                    try (final InputStream is = zipFile.getInputStream(entry)) {
                        assertNotNull(is);
                    }
                }
            }

            assertEquals(16, fileCount, "Should have 16 files");
        }
    }

    @Test
    void testDeepNesting() throws IOException {
        try (final var zipFile = ZipFile.builder().setURI(ZIP_FILE_URI).get()) {
            final var entry = zipFile.getEntry("data/nested/deep/level3.json");
            assertNotNull(entry, "Deep nested file should exist");
            assertFalse(entry.isDirectory());

            try (final InputStream is = zipFile.getInputStream(entry)) {
                assertNotNull(is);
                final byte[] content = is.readAllBytes();
                final CRC32 crc32 = new CRC32();
                crc32.update(content);
                assertEquals(LEVEL3_JSON_CRC32, crc32.getValue(), CHECKSUM_MISMATCH);
            }
        }
    }

    @Test
    void testEmptyFile() throws IOException {
        try (final var zipFile = ZipFile.builder().setURI(ZIP_FILE_URI).get()) {
            final ZipArchiveEntry entry = zipFile.getEntry("empty_file.txt");
            assertNotNull(entry);
            assertEquals(0, entry.getSize());

            try (InputStream is = zipFile.getInputStream(entry)) {
                byte[] content = is.readAllBytes();
                assertEquals(0, content.length);
            }
        }
    }

    @Test
    void testFileWithSpaces() throws IOException {
        try (final var zipFile = ZipFile.builder().setURI(ZIP_FILE_URI).get()) {
            final ZipArchiveEntry entry = zipFile.getEntry("file with spaces.txt");
            assertNotNull(entry, "File with spaces should exist");

            try (final InputStream is = zipFile.getInputStream(entry)) {
                final byte[] content = is.readAllBytes();
                final CRC32 crc32 = new CRC32();
                crc32.update(content);
                assertEquals(SPACES_TXT_CRC32, crc32.getValue(), CHECKSUM_MISMATCH);
            }
        }
    }

    @Test
    void testHiddenFile() throws IOException {
        try (final var zipFile = ZipFile.builder().setURI(ZIP_FILE_URI).get()) {
            final ZipArchiveEntry entry = zipFile.getEntry(".hidden_file");
            assertNotNull(entry, "Hidden file should exist");
        }
    }

    @Test
    void testCaseSensitivity() throws IOException {
        try (final var zipFile = ZipFile.builder().setURI(ZIP_FILE_URI).get()) {
            assertNotNull(zipFile.getEntry("UPPERCASE.HTML"));
            assertNotNull(zipFile.getEntry("index.html"));
            assertNull(zipFile.getEntry("uppercase.html"),
                "ZIP should be case-sensitive");
        }
    }

    @Test
    void testLargeFileStreaming() throws IOException {
        try (final var zipFile = ZipFile.builder().setURI(ZIP_FILE_URI).get()) {
            final ZipArchiveEntry entry = zipFile.getEntry("data/large_dummy.bin");
            assertNotNull(entry);
            assertEquals(65536, entry.getSize());

            try (final InputStream is = zipFile.getInputStream(entry)) {
                final byte[] buffer = new byte[1024];
                int totalRead = 0;
                int bytesRead;
                final CRC32 crc32 = new CRC32();

                while ((bytesRead = is.read(buffer)) != -1) {
                    crc32.update(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }

                assertEquals(65536, totalRead);
                assertEquals(LARGE_DUMMY_BIN_CRC32, crc32.getValue(), CHECKSUM_MISMATCH);
            }
        }
    }
    
    @Test
    void testDirectoryEntriesSkipped() throws IOException {
        try (final var zipFile = ZipFile.builder().setURI(ZIP_FILE_URI).get()) {
            final Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (entry.getName().equals("css/")) {
                    assertTrue(entry.isDirectory(), "css/ should be a directory");
                }
            }
        }
    }
    
    @Test
    void testAllureReportStructure() throws IOException {
        try (final var zipFile = ZipFile.builder().setURI(ZIP_FILE_URI).get()) {
            assertNotNull(zipFile.getEntry("allure-report/index.html"));
            assertNotNull(zipFile.getEntry("allure-report/widgets/summary.json"));
        }
    }
}
