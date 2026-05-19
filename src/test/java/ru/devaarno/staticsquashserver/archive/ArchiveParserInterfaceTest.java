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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for ArchiveParser implementations using test archives.
 * Tests all ArchiveParser interface methods across ZIP, TAR.GZ, and TAR.XZ formats.
 */
class ArchiveParserInterfaceTest {

    private static final Path ZIP_ARCHIVE = assertDoesNotThrow(
            () -> Path.of(Objects.requireNonNull(
                    ArchiveParserInterfaceTest.class.getResource("/test_archives/test.zip")
            ).toURI()),
            "Test TAR.XZ archive URI should be resolved without exceptions"
    );

    private static final Path TAR_GZ_ARCHIVE = assertDoesNotThrow(
            () -> Path.of(Objects.requireNonNull(
                    ArchiveParserInterfaceTest.class.getResource("/test_archives/test.tar.gz")
            ).toURI()),
            "Test TAR.XZ archive URI should be resolved without exceptions"
    );

    private static final Path TAR_XZ_ARCHIVE = assertDoesNotThrow(
            () -> Path.of(Objects.requireNonNull(
                    ArchiveParserInterfaceTest.class.getResource("/test_archives/test.tar.xz")
            ).toURI()),
            "Test TAR.XZ archive URI should be resolved without exceptions"
    );

    private static final long SPACES_TXT_CRC32 = 832580676L;
    private static final long LARGE_DUMMY_BIN_CRC32 = 2694514304L;
    private static final long LEVEL3_JSON_CRC32 = 4118783144L;
    private static final String CHECKSUM_MISMATCH = "CRC32 checksum mismatch";

    @Test
    void testZipParserFormat() {
        ArchiveParser parser = ArchiveParserFactory.create(ZIP_ARCHIVE);
        assertEquals(ArchiveFormat.ZIP, parser.format());
    }

    @Test
    void testTarGzParserFormat() {
        ArchiveParser parser = ArchiveParserFactory.create(TAR_GZ_ARCHIVE);
        assertEquals(ArchiveFormat.TAR_GZ, parser.format());
    }

    @Test
    void testTarXzParserFormat() {
        ArchiveParser parser = ArchiveParserFactory.create(TAR_XZ_ARCHIVE);
        assertEquals(ArchiveFormat.TAR_XZ, parser.format());
    }

    @ParameterizedTest
    @EnumSource(ArchiveFormat.class)
    void testInitScanReturnsCorrectFileCount(ArchiveFormat format) {
        Path archive = getArchivePath(format);
        ArchiveParser parser = ArchiveParserFactory.create(archive);
        ArchiveDescriptor descriptor = parser.initScan();

        assertEquals(16, descriptor.entries().size(), 
            "Should have 16 files in " + format + " archive");
        assertEquals(format, descriptor.format());
        assertEquals(archive, descriptor.archivePath());
    }

    @ParameterizedTest
    @EnumSource(ArchiveFormat.class)
    void testInitScanContainsAllExpectedFiles(ArchiveFormat format) {
        Path archive = getArchivePath(format);
        ArchiveParser parser = ArchiveParserFactory.create(archive);
        ArchiveDescriptor descriptor = parser.initScan();

        List<String> entryPaths = descriptor.entries().stream()
            .map(ArchiveEntryInfo::path)
            .toList();

        assertTrue(entryPaths.contains("index.html"), "index.html should be present");
        assertTrue(entryPaths.contains("empty_file.txt"), "empty_file.txt should be present");
        assertTrue(entryPaths.contains("file with spaces.txt"), "file with spaces.txt should be present");
        assertTrue(entryPaths.contains("special-chars_@#.txt"), "special-chars_@#.txt should be present");
        assertTrue(entryPaths.contains("UPPERCASE.HTML"), "UPPERCASE.HTML should be present");
        assertTrue(entryPaths.contains(".hidden_file"), ".hidden_file should be present");
        assertTrue(entryPaths.contains("css/style.css"), "css/style.css should be present");
        assertTrue(entryPaths.contains("js/app.js"), "js/app.js should be present");
        assertTrue(entryPaths.contains("js/lib/vendor.js"), "js/lib/vendor.js should be present");
        assertTrue(entryPaths.contains("data/report.json"), "data/report.json should be present");
        assertTrue(entryPaths.contains("data/large_dummy.bin"), "data/large_dummy.bin should be present");
        assertTrue(entryPaths.contains("data/nested/deep/level3.json"), "data/nested/deep/level3.json should be present");
        assertTrue(entryPaths.contains("images/logo.png"), "images/logo.png should be present");
        assertTrue(entryPaths.contains("images/icons/small.svg"), "images/icons/small.svg should be present");
        assertTrue(entryPaths.contains("allure-report/index.html"), "allure-report/index.html should be present");
        assertTrue(entryPaths.contains("allure-report/widgets/summary.json"), "allure-report/widgets/summary.json should be present");
    }

    @ParameterizedTest
    @EnumSource(ArchiveFormat.class)
    void testInitScanEntrySizes(ArchiveFormat format) {
        Path archive = getArchivePath(format);
        ArchiveParser parser = ArchiveParserFactory.create(archive);
        ArchiveDescriptor descriptor = parser.initScan();

        ArchiveEntryInfo emptyFile = descriptor.entries().stream()
            .filter(e -> e.path().equals("empty_file.txt"))
            .findFirst()
            .orElseThrow();
        assertEquals(0, emptyFile.size(), "empty_file.txt should be 0 bytes");

        ArchiveEntryInfo largeFile = descriptor.entries().stream()
            .filter(e -> e.path().equals("data/large_dummy.bin"))
            .findFirst()
            .orElseThrow();
        assertEquals(65536, largeFile.size(), "data/large_dummy.bin should be 65536 bytes");
    }

    @ParameterizedTest
    @EnumSource(ArchiveFormat.class)
    void testFillOutput(ArchiveFormat format) throws Exception {
        Path archive = getArchivePath(format);
        ArchiveParser parser = ArchiveParserFactory.create(archive);

        try (final var byteArrayOutputStream = new ByteArrayOutputStream()) {
            parser.fillOutput("index.html", byteArrayOutputStream);
            final var bytes = byteArrayOutputStream.toByteArray();
            assertNotNull(bytes, "index.html stream should not be null");
            assertTrue(bytes.length > 0, "index.html should have content");
        }
    }

    @ParameterizedTest
    @EnumSource(ArchiveFormat.class)
    void testFillOutputDeepNested(ArchiveFormat format) throws Exception {
        Path archive = getArchivePath(format);
        ArchiveParser parser = ArchiveParserFactory.create(archive);

        try (final var byteArrayOutputStream = new ByteArrayOutputStream()) {
            parser.fillOutput("data/nested/deep/level3.json", byteArrayOutputStream);
            final var bytes = byteArrayOutputStream.toByteArray();
            assertNotNull(bytes, "level3.json stream should not be null");
            final CRC32 crc32 = new CRC32();
            crc32.update(bytes);
            assertEquals(LEVEL3_JSON_CRC32, crc32.getValue(), CHECKSUM_MISMATCH);
        }
    }

    @ParameterizedTest
    @EnumSource(ArchiveFormat.class)
    void testFillOutputFileWithSpaces(ArchiveFormat format) throws Exception {
        Path archive = getArchivePath(format);
        ArchiveParser parser = ArchiveParserFactory.create(archive);

        try (final var byteArrayOutputStream = new ByteArrayOutputStream()) {
            parser.fillOutput("file with spaces.txt", byteArrayOutputStream);
            final var bytes = byteArrayOutputStream.toByteArray();
            assertNotNull(bytes, "file with spaces.txt stream should not be null");
            final CRC32 crc32 = new CRC32();
            crc32.update(bytes);
            assertEquals(SPACES_TXT_CRC32, crc32.getValue(), CHECKSUM_MISMATCH);
        }
    }

    @ParameterizedTest
    @EnumSource(ArchiveFormat.class)
    void testFillOutputLargeFile(ArchiveFormat format) throws Exception {
        Path archive = getArchivePath(format);
        ArchiveParser parser = ArchiveParserFactory.create(archive);

        try (final var byteArrayOutputStream = new ByteArrayOutputStream()) {
            parser.fillOutput("data/large_dummy.bin", byteArrayOutputStream);
            final var bytes = byteArrayOutputStream.toByteArray();
            assertNotNull(bytes, "large_dummy.bin stream should not be null");
            assertEquals(65536, bytes.length, "large_dummy.bin should be 65536 bytes");
            final CRC32 crc32 = new CRC32();
            crc32.update(bytes);
            assertEquals(LARGE_DUMMY_BIN_CRC32, crc32.getValue(), CHECKSUM_MISMATCH);
        }
    }

    @ParameterizedTest
    @EnumSource(ArchiveFormat.class)
    void testFillOutputNotFound(ArchiveFormat format)  throws Exception {
        Path archive = getArchivePath(format);
        ArchiveParser parser = ArchiveParserFactory.create(archive);

        try (final var byteArrayOutputStream = new ByteArrayOutputStream()) {
            assertThrowsExactly(
                    FileNotFoundException.class,
                    () -> parser.fillOutput("nonexistent_file.txt", byteArrayOutputStream)
            );
        }
    }

    @ParameterizedTest
    @EnumSource(ArchiveFormat.class)
    void testMediaTypeDetection(ArchiveFormat format) {
        Path archive = getArchivePath(format);
        ArchiveParser parser = ArchiveParserFactory.create(archive);
        ArchiveDescriptor descriptor = parser.initScan();

        ArchiveEntryInfo htmlEntry = descriptor.entries().stream()
            .filter(e -> e.path().equals("index.html"))
            .findFirst()
            .orElseThrow();
        assertEquals(io.helidon.common.media.type.MediaTypes.TEXT_HTML, htmlEntry.mediaType());

        ArchiveEntryInfo cssEntry = descriptor.entries().stream()
            .filter(e -> e.path().equals("css/style.css"))
            .findFirst()
            .orElseThrow();
        assertEquals(io.helidon.common.media.type.MediaTypes.create("text/css"), cssEntry.mediaType());

        ArchiveEntryInfo jsEntry = descriptor.entries().stream()
            .filter(e -> e.path().equals("js/app.js"))
            .findFirst()
            .orElseThrow();
        assertEquals("text/javascript", jsEntry.mediaType().text());

        ArchiveEntryInfo jsonEntry = descriptor.entries().stream()
            .filter(e -> e.path().equals("data/report.json"))
            .findFirst()
            .orElseThrow();
        assertEquals(io.helidon.common.media.type.MediaTypes.APPLICATION_JSON, jsonEntry.mediaType());

        ArchiveEntryInfo pngEntry = descriptor.entries().stream()
            .filter(e -> e.path().equals("images/logo.png"))
            .findFirst()
            .orElseThrow();
        assertEquals(io.helidon.common.media.type.MediaTypes.create("image/png"), pngEntry.mediaType());

        ArchiveEntryInfo svgEntry = descriptor.entries().stream()
            .filter(e -> e.path().equals("images/icons/small.svg"))
            .findFirst()
            .orElseThrow();
        assertEquals(io.helidon.common.media.type.MediaTypes.create("image/svg+xml"), svgEntry.mediaType());
    }

    @ParameterizedTest
    @EnumSource(ArchiveFormat.class)
    void testAllureReportEntries(ArchiveFormat format) {
        Path archive = getArchivePath(format);
        ArchiveParser parser = ArchiveParserFactory.create(archive);
        ArchiveDescriptor descriptor = parser.initScan();

        List<String> entryPaths = descriptor.entries().stream()
            .map(ArchiveEntryInfo::path)
            .toList();

        assertTrue(entryPaths.contains("allure-report/index.html"));
        assertTrue(entryPaths.contains("allure-report/widgets/summary.json"));
    }

    @Test
    void testInvalidArchiveFormatThrowsException() {
        Path invalidArchive = Path.of("src/test/resources/test_archives/invalid.xyz");
        assertThrows(IllegalArgumentException.class, () -> 
            ArchiveParserFactory.create(invalidArchive)
        );
    }

    private Path getArchivePath(ArchiveFormat format) {
        return switch (format) {
            case ZIP -> ZIP_ARCHIVE;
            case TAR_GZ -> TAR_GZ_ARCHIVE;
            case TAR_XZ -> TAR_XZ_ARCHIVE;
        };
    }
}
