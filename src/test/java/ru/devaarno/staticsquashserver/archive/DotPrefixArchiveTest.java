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
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for archives whose entries carry a leading {@code ./} prefix, as produced by
 * {@code tar -cf archive.tar ./dir}. Browsers collapse such dot segments per RFC 3986,
 * so the server must expose the canonical (dot-free) URL path while matching content
 * by the raw archive name.
 */
class DotPrefixArchiveTest {

    private static final Path DOT_TEST_ARCHIVE = assertDoesNotThrow(
            () -> Path.of(Objects.requireNonNull(
                    DotPrefixArchiveTest.class.getResource("/test_archives/dot_test.tar.xz")
            ).toURI()),
            "Test TAR.XZ archive URI should be resolved without exceptions"
    );

    private static final long HELLO_TXT_CRC32 = 831927574L;
    private static final long WORLD_TXT_CRC32 = 3711459752L;
    private static final String CHECKSUM_MISMATCH = "CRC32 checksum mismatch";

    @Test
    void testInitScanPreservesRawDotPrefixedPaths() {
        final var parser = ArchiveParserFactory.create(DOT_TEST_ARCHIVE);
        final var descriptor = parser.initScan();

        final var entryPaths = descriptor.entries().stream()
                .map(ArchiveEntryInfo::path)
                .toList();

        assertEquals(3, entryPaths.size(), "Should have 3 files");
        assertTrue(entryPaths.contains("./README.md"), "Raw path should keep the ./ prefix");
        assertTrue(entryPaths.contains("./a/hello.txt"), "Raw path should keep the ./ prefix");
        assertTrue(entryPaths.contains("./b/world.txt"), "Raw path should keep the ./ prefix");
    }

    @Test
    void testUrlPathStripsDotPrefix() {
        final var parser = ArchiveParserFactory.create(DOT_TEST_ARCHIVE);
        final var descriptor = parser.initScan();

        final var urlPaths = descriptor.entries().stream()
                .map(ArchiveEntryInfo::urlPath)
                .toList();

        assertTrue(urlPaths.contains("README.md"), "Canonical path should drop the ./ prefix");
        assertTrue(urlPaths.contains("a/hello.txt"), "Canonical path should drop the ./ prefix");
        assertTrue(urlPaths.contains("b/world.txt"), "Canonical path should drop the ./ prefix");
    }

    @Test
    void testFillOutputByRawPath() throws Exception {
        final var parser = ArchiveParserFactory.create(DOT_TEST_ARCHIVE);

        try (final var byteArrayOutputStream = new ByteArrayOutputStream()) {
            parser.fillOutput("./a/hello.txt", byteArrayOutputStream);
            final CRC32 crc32 = new CRC32();
            crc32.update(byteArrayOutputStream.toByteArray());
            assertEquals(HELLO_TXT_CRC32, crc32.getValue(), CHECKSUM_MISMATCH);
        }

        try (final var byteArrayOutputStream = new ByteArrayOutputStream()) {
            parser.fillOutput("./b/world.txt", byteArrayOutputStream);
            final CRC32 crc32 = new CRC32();
            crc32.update(byteArrayOutputStream.toByteArray());
            assertEquals(WORLD_TXT_CRC32, crc32.getValue(), CHECKSUM_MISMATCH);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "./foo/bar.html, foo/bar.html",
            "a/./b/../c, a/c",
            "a//b///c, a/b/c",
            "dir\\file.txt, dir/file.txt",
            "/etc/passwd, etc/passwd",
            "./README.md, README.md"
    })
    void testCanonicalizeUrlPath(final String raw, final String expected) {
        assertEquals(expected, ArchiveParser.canonicalizeUrlPath(raw), "Canonical path should match for " + raw);
    }
}
