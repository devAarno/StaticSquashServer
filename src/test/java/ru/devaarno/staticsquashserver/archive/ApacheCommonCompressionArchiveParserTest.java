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
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ArchiveParser implementations.
 * Tests edge cases in tree structure handling.
 */
class ApacheCommonCompressionArchiveParserTest {

    private static final URI TAR_GZ_FILE_URI = assertDoesNotThrow(
            () -> Objects.requireNonNull(
                    ApacheCommonCompressionArchiveParserTest.class.getResource("/test_archives/test.tar.gz")
            ).toURI(),
            "Test TAR.GZ archive URI should be resolved without exceptions"
    );

    private static final URI TAR_XZ_FILE_URI = assertDoesNotThrow(
            () -> Objects.requireNonNull(
                    ApacheCommonCompressionArchiveParserTest.class.getResource("/test_archives/test.tar.xz")
            ).toURI(),
            "Test TAR.XZ archive URI should be resolved without exceptions"
    );

    private static final long SPACES_TXT_CRC32 = 832580676L;
    private static final long LARGE_DUMMY_BIN_CRC32 = 2694514304L;
    private static final long LEVEL3_JSON_CRC32 = 4118783144L;
    private static final String CHECKSUM_MISMATCH = "CRC32 checksum mismatch";

    record StreamHandler(
            InputStream fileStream,
            CompressorInputStream compressorInputStream,
            TarArchiveInputStream tarArchiveInputStream
    ) implements Closeable  {
        @Override
        public void close() throws IOException {
            tarArchiveInputStream.close();
            compressorInputStream.close();
            fileStream.close();
        }
    }

    static StreamHandler openTarGz() {
        final var fileStream = assertDoesNotThrow(
                () -> Files.newInputStream(Path.of(TAR_GZ_FILE_URI))
        );
        final var compressorInputStream = assertDoesNotThrow(
                () -> new GzipCompressorInputStream(fileStream)
        );
        final var tarStream = new TarArchiveInputStream(compressorInputStream);
        return new StreamHandler(fileStream, compressorInputStream, tarStream);
    }

    static StreamHandler openTarXz() {
        final var fileStream = assertDoesNotThrow(
                () -> Files.newInputStream(Path.of(TAR_XZ_FILE_URI))
        );
        final var gzStream = assertDoesNotThrow(
                () -> new XZCompressorInputStream(fileStream)
        );
        final var tarStream = new TarArchiveInputStream(gzStream);
        return new StreamHandler(fileStream, gzStream, tarStream);
    }

    static Stream<TarArchiveInputStream> openedTars() {
        final var tarGz = openTarGz();
        final var tarXz = openTarXz();
        return Stream
                .of(tarGz, tarXz)
                .map(StreamHandler::tarArchiveInputStream)
                .onClose(
                        () -> assertDoesNotThrow(() -> {
                            tarGz.close();
                            tarXz.close();
                        })
                );
    }

    @ParameterizedTest
    @MethodSource("openedTars")
    void testArchiveParsing(final TarArchiveInputStream tarStream) {
        int fileCount = 0;
        TarArchiveEntry entry;

        while ((entry = assertDoesNotThrow(tarStream::getNextEntry)) != null) {

            assertFalse(entry.getName().startsWith("/"),
                    "Entry name should not start with /: " + entry.getName());

            if (!entry.isDirectory()) {
                fileCount++;
            }
        }

        assertEquals(16, fileCount, "Should have 16 files");
    }

    @ParameterizedTest
    @MethodSource("openedTars")
    void testDeepNesting(final TarArchiveInputStream tarStream) {
        final var fileIsFound = assertDoesNotThrow(
                () -> {
                    while ((tarStream.getNextEntry()) != null) {
                        final var entry = tarStream.getCurrentEntry();
                        if (entry.isFile() && entry.getName().equals("data/nested/deep/level3.json")) {
                            final CRC32 crc32 = new CRC32();
                            crc32.update(tarStream.readAllBytes());
                            assertEquals(LEVEL3_JSON_CRC32, crc32.getValue(), CHECKSUM_MISMATCH);
                            return true;
                        }
                    }
                    return false;
                }
        );
        assertTrue(fileIsFound, "File should be found");
    }

    @ParameterizedTest
    @MethodSource("openedTars")
    void testEmptyFile(final TarArchiveInputStream tarStream) {
        final var fileIsFound = assertDoesNotThrow(
                () -> {
                    while ((tarStream.getNextEntry()) != null) {
                        final var entry = tarStream.getCurrentEntry();
                        if (entry.isFile() && entry.getName().equals("empty_file.txt")) {
                            assertEquals(0, tarStream.readAllBytes().length, "Empty file content length should be 0");
                            return true;
                        }
                    }
                    return false;
                }
        );
        assertTrue(fileIsFound, "File should be found");
    }

    @ParameterizedTest
    @MethodSource("openedTars")
    void testFileWithSpaces(final TarArchiveInputStream tarStream) {
        final var fileIsFound = assertDoesNotThrow(
                () -> {
                    while ((tarStream.getNextEntry()) != null) {
                        final var entry = tarStream.getCurrentEntry();
                        if (entry.isFile() && entry.getName().equals("file with spaces.txt")) {
                            final CRC32 crc32 = new CRC32();
                            crc32.update(tarStream.readAllBytes());
                            assertEquals(SPACES_TXT_CRC32, crc32.getValue(), CHECKSUM_MISMATCH);
                            return true;
                        }
                    }
                    return false;
                }
        );
        assertTrue(fileIsFound, "File should be found");
    }

    @ParameterizedTest
    @MethodSource("openedTars")
    void testHiddenFile(final TarArchiveInputStream tarStream) {
        final var fileIsFound = assertDoesNotThrow(
                () -> {
                    while ((tarStream.getNextEntry()) != null) {
                        final var entry = tarStream.getCurrentEntry();
                        if (entry.isFile() && entry.getName().equals(".hidden_file")) {
                            return true;
                        }
                    }
                    return false;
                }
        );
        assertTrue(fileIsFound, "File should be found");
    }

    private record Marks(
            boolean capitalUpperCaseIsFound,
            boolean indexIsFound,
            boolean lowerUpperCaseIsFound
    ) {}

    @ParameterizedTest
    @MethodSource("openedTars")
    void testCaseSensitivity(final TarArchiveInputStream tarStream) {
        final var marks = assertDoesNotThrow(
                () -> {
                    boolean capitalUpperCaseIsFound = false;
                    boolean indexIsFound = false;
                    boolean lowerUpperCaseIsFound = false;
                    while ((tarStream.getNextEntry()) != null) {
                        final var entry = tarStream.getCurrentEntry();
                        if (entry.isFile() && entry.getName().equals("UPPERCASE.HTML")) {
                            capitalUpperCaseIsFound = true;
                        }
                        if (entry.isFile() && entry.getName().equals("index.html")) {
                            indexIsFound = true;
                        }
                        if (entry.isFile() && entry.getName().equals("uppercase.html")) {
                            lowerUpperCaseIsFound = true;
                        }
                    }
                    return new Marks(capitalUpperCaseIsFound, indexIsFound, lowerUpperCaseIsFound);
                }
        );
        assertAll(
                () -> assertTrue(marks.capitalUpperCaseIsFound(), "UPPERCASE.HTML should be found"),
                () -> assertTrue(marks.indexIsFound(), "index.html should be found"),
                () -> assertFalse(marks.lowerUpperCaseIsFound(), "uppercase.html should be found")
        );
    }

    @ParameterizedTest
    @MethodSource("openedTars")
    void testLargeFileStreaming(final TarArchiveInputStream tarStream) {
        final var fileIsFound = assertDoesNotThrow(
                () -> {
                    while ((tarStream.getNextEntry()) != null) {
                        final var entry = tarStream.getCurrentEntry();
                        if (entry.isFile() && entry.getName().equals("data/large_dummy.bin")) {
                            final byte[] buffer = tarStream.readAllBytes();
                            assertEquals(65536, buffer.length, "Large dummy file buffer length should be 65536");

                            final CRC32 crc32 = new CRC32();
                            crc32.update(buffer, 0, buffer.length);
                            assertEquals(LARGE_DUMMY_BIN_CRC32, crc32.getValue(), CHECKSUM_MISMATCH);
                            return true;
                        }
                    }
                    return false;
                }
        );
        assertTrue(fileIsFound, "File should be found");
    }

    @ParameterizedTest
    @MethodSource("openedTars")
    void testDirectoryEntriesSkipped(final TarArchiveInputStream tarStream) {
        final var fileIsFound = assertDoesNotThrow(
                () -> {
                    while ((tarStream.getNextEntry()) != null) {
                        final var entry = tarStream.getCurrentEntry();
                        if (entry.isDirectory() && entry.getName().equals("css/")) {
                            return true;
                        }
                    }
                    return false;
                }
        );
        assertTrue(fileIsFound, "Directory css/ should be found");
    }

    private record AllureMarks(
            boolean indexIsFound,
            boolean summaryIsFound
    ) {}

    @ParameterizedTest
    @MethodSource("openedTars")
    void testAllureReportStructure(final TarArchiveInputStream tarStream) {
        final var marks = assertDoesNotThrow(
                () -> {
                    boolean indexIsFound = false;
                    boolean summaryIsFound = false;
                    while ((tarStream.getNextEntry()) != null) {
                        final var entry = tarStream.getCurrentEntry();
                        if (entry.isFile() && entry.getName().equals("allure-report/index.html")) {
                            indexIsFound = true;
                        }
                        if (entry.isFile() && entry.getName().equals("allure-report/widgets/summary.json")) {
                            summaryIsFound = true;
                        }
                    }
                    return new AllureMarks(indexIsFound, summaryIsFound);
                }
        );
        assertAll(
                () -> assertTrue(marks.indexIsFound(), "allure-report/index.html should be found"),
                () -> assertTrue(marks.summaryIsFound(), "allure-report/widgets/summary.json should be found")
        );
    }
}
