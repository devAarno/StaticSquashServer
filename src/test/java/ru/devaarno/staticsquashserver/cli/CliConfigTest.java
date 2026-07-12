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

package ru.devaarno.staticsquashserver.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliConfigTest {

    @Test
    void testDefaultPort(final @TempDir Path tempDir) throws IOException {
        final var archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);

        String[] args = {"--archive", archiveFile.toString()};
        final var config = CliConfig.parse(args);

        assertEquals(8080, config.getPort(), "Default port should be 8080");
    }

    @Test
    void testCustomPort(final @TempDir Path tempDir) throws IOException {
        final var archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);

        String[] args = {"--port", "9090", "--archive", archiveFile.toString()};
        final var config = CliConfig.parse(args);

        assertEquals(9090, config.getPort(), "Custom port should be 9090");
    }

    @Test
    void testShortPortOption(final @TempDir Path tempDir) throws IOException {
        final var archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);

        String[] args = {"-p", "8888", "-a", archiveFile.toString()};
        final var config = CliConfig.parse(args);

        assertEquals(8888, config.getPort(), "Short port option should be 8888");
        assertEquals(archiveFile, config.getArchivePath(), "Archive path should match");
    }

    @Test
    void testArchivePath(final @TempDir Path tempDir) throws IOException {
        final var archiveFile = tempDir.resolve("archive.tar.gz");
        Files.createFile(archiveFile);

        String[] args = {"--archive", archiveFile.toString()};
        final var config = CliConfig.parse(args);

        assertEquals(archiveFile, config.getArchivePath(), "Archive path should match");
    }

    @Test
    void testMissingArchivePath(final @TempDir Path tempDir) throws IOException {
        final var archiveFile = tempDir.resolve("archive.zip");
        Files.createFile(archiveFile);

        String[] args = {"--archive", archiveFile.toString()};
        final var config = CliConfig.parse(args);

        assertEquals(archiveFile, config.getArchivePath(), "Archive path should match");
    }

    @Test
    void testArchivePathRequired() {
        String[] args = {"--port", "8080"};

        final var exception = assertThrows(
                ParameterException.class,
                () -> CliConfig.parse(args),
                "Should throw ParameterException when archive path is missing"
        );
        assertEquals("The following option is required: [-a | --archive]", exception.getMessage());
    }

    @Test
    void testArchiveFileMustExist() {
        String[] args = {"--archive", "/nonexistent/path/archive.zip"};

        final var exception = assertThrows(
                ParameterException.class,
                () -> CliConfig.parse(args),
                "Should throw ParameterException when file does not exist"
        );
        assertTrue(
                exception.getMessage().startsWith("File is not existed"),
                "Exception message should start with 'File is not existed'"
        );
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testArchiveFileMustBeReadable(final @TempDir Path tempDir) throws IOException {
        final var archiveFile = tempDir.resolve("archive.zip");
        Files.createFile(archiveFile);
        Files.setPosixFilePermissions(archiveFile, java.nio.file.attribute.PosixFilePermissions.fromString("---------"));

        String[] args = {"--archive", archiveFile.toString()};

        final var exception = assertThrows(
                ParameterException.class,
                () -> CliConfig.parse(args),
                "Should throw ParameterException when file is not readable"
        );
        assertTrue(
                exception.getMessage().contains("not readable"),
                "Exception message should contain 'not readable'"
        );
    }

    @Test
    void testPortOutOfRangeLow(final @TempDir Path tempDir) throws IOException {
        final var archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);

        String[] args = {"--port", "0", "--archive", archiveFile.toString()};

        final var exception = assertThrows(ParameterException.class, () -> CliConfig.parse(args),
                "Should throw ParameterException when port is too low");
        assertTrue(
                exception.getMessage().contains("Port must be between"),
                "Exception message should contain 'Port must be between'"
        );
    }

    @Test
    void testPortOutOfRangeHigh(final @TempDir Path tempDir) throws IOException {
        final var archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);
        String[] args = {"--port", "65536", "--archive", archiveFile.toString()};

        final var exception = assertThrows(ParameterException.class, () -> CliConfig.parse(args),
                "Should throw ParameterException when port is too high");
        assertTrue(
                exception.getMessage().contains("Port must be between"),
                "Exception message should contain 'Port must be between'"
        );
    }

    @Test
    void testValidPortBoundaryMin(final @TempDir Path tempDir) throws IOException {
        final var archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);

        String[] args = {"--port", "1", "--archive", archiveFile.toString()};

        final var config = CliConfig.parse(args);
        assertEquals(1, config.getPort(), "Minimum valid port should be 1");
    }

    @Test
    void testValidPortBoundaryMax(final @TempDir Path tempDir) throws IOException {
        final var archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);

        String[] args = {"--port", "65535", "--archive", archiveFile.toString()};

        final var config = CliConfig.parse(args);
        assertEquals(65535, config.getPort(), "Maximum valid port should be 65535");
    }

    @Test
    void testSupportsTarGzArchive(final @TempDir Path tempDir) throws IOException {
        final var archiveFile = tempDir.resolve("archive.tar.gz");
        Files.createFile(archiveFile);

        String[] args = {"--archive", archiveFile.toString()};

        final var config = CliConfig.parse(args);
        assertTrue(
                String.valueOf(config.getArchivePath()).endsWith(".tar.gz"),
                "Archive path should end with .tar.gz"
        );
    }

    @Test
    void testSupportsXzArchive(final @TempDir Path tempDir) throws IOException {
        final var archiveFile = tempDir.resolve("archive.tar.xz");
        Files.createFile(archiveFile);

        String[] args = {"--archive", archiveFile.toString()};

        final var config = CliConfig.parse(args);
        assertTrue(
                String.valueOf(config.getArchivePath()).endsWith(".tar.xz"),
                "Archive path should end with .tar.xz"
        );
    }

    @Test
    void testUsageHelpOutput() {

        final var jc = JCommander
                .newBuilder()
                .addObject(new CliConfig())
                .build();

        final var sb = new StringBuilder(128);
        jc.usage(sb);

        final var helpOutput = sb.toString();
        assertTrue(helpOutput.contains("-p, --port"));
        assertTrue(helpOutput.contains("* -a, --archive"));
        assertTrue(helpOutput.contains("-h, --help"));
    }

    @Test
    void testUsageHelpCall() {
        // This test fills `reachability-metadata.json`
        final var config = assertDoesNotThrow(
                () -> CliConfig.parse(new String[] {"-h"})
        );
        assertTrue(config.isHelp());
    }
}
