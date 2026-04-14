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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ArchiveParser implementations.
 * Tests edge cases in tree structure handling.
 */
class ArchiveParserTest {
    
    private static final Path TEST_DIR = Paths.get("target/test-classes");
    
    @Test
    void testZipParsing() throws IOException {
        Path zipPath = TEST_DIR.resolve("test_archives/test.zip");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            int fileCount = 0;
            
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                assertFalse(entry.getName().startsWith("/"), 
                    "Entry name should not start with /: " + entry.getName());
                
                if (!entry.isDirectory()) {
                    fileCount++;
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        assertNotNull(is);
                    }
                }
            }
            
            assertEquals(16, fileCount, "Should have 16 files");
        }
    }
    
    @Test
    void testDeepNesting() throws IOException {
        Path zipPath = TEST_DIR.resolve("test_archives/test.zip");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipArchiveEntry entry = zipFile.getEntry("data/nested/deep/level3.json");
            assertNotNull(entry, "Deep nested file should exist");
            assertFalse(entry.isDirectory());
            
            try (InputStream is = zipFile.getInputStream(entry)) {
                assertNotNull(is);
                byte[] content = is.readAllBytes();
                assertTrue(content.length > 0);
            }
        }
    }
    
    @Test
    void testEmptyFile() throws IOException {
        Path zipPath = TEST_DIR.resolve("test_archives/test.zip");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipArchiveEntry entry = zipFile.getEntry("empty_file.txt");
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
        Path zipPath = TEST_DIR.resolve("test_archives/test.zip");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipArchiveEntry entry = zipFile.getEntry("file with spaces.txt");
            assertNotNull(entry, "File with spaces should exist");
            
            try (InputStream is = zipFile.getInputStream(entry)) {
                byte[] content = is.readAllBytes();
                assertTrue(content.length > 0);
            }
        }
    }
    
    @Test
    void testHiddenFile() throws IOException {
        Path zipPath = TEST_DIR.resolve("test_archives/test.zip");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipArchiveEntry entry = zipFile.getEntry(".hidden_file");
            assertNotNull(entry, "Hidden file should exist");
        }
    }
    
    @Test
    void testCaseSensitivity() throws IOException {
        Path zipPath = TEST_DIR.resolve("test_archives/test.zip");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            assertNotNull(zipFile.getEntry("UPPERCASE.HTML"));
            assertNotNull(zipFile.getEntry("index.html"));
            assertNull(zipFile.getEntry("uppercase.html"), 
                "ZIP should be case-sensitive");
        }
    }
    
    @Test
    void testLargeFileStreaming() throws IOException {
        Path zipPath = TEST_DIR.resolve("test_archives/test.zip");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipArchiveEntry entry = zipFile.getEntry("data/large_dummy.bin");
            assertNotNull(entry);
            assertEquals(65536, entry.getSize());
            
            try (InputStream is = zipFile.getInputStream(entry)) {
                byte[] buffer = new byte[1024];
                int totalRead = 0;
                int bytesRead;
                
                while ((bytesRead = is.read(buffer)) != -1) {
                    totalRead += bytesRead;
                }
                
                assertEquals(65536, totalRead);
            }
        }
    }
    
    @Test
    void testDirectoryEntriesSkipped() throws IOException {
        Path zipPath = TEST_DIR.resolve("test_archives/test.zip");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            
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
        Path zipPath = TEST_DIR.resolve("test_archives/test.zip");
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            assertNotNull(zipFile.getEntry("allure-report/index.html"));
            assertNotNull(zipFile.getEntry("allure-report/widgets/summary.json"));
        }
    }
}
