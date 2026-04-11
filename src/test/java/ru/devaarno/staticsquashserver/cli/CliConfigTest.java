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

import com.beust.jcommander.ParameterException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CliConfigTest {

    @Test
    void testDefaultPort(@TempDir Path tempDir) throws IOException {
        Path archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);
        String[] args = {"--archive", archiveFile.toString()};
        CliConfig config = CliConfig.parse(args);

        assertThat(config.getPort()).isEqualTo(8080);
    }

    @Test
    void testCustomPort(@TempDir Path tempDir) throws IOException {
        Path archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);
        String[] args = {"--port", "9090", "--archive", archiveFile.toString()};
        CliConfig config = CliConfig.parse(args);

        assertThat(config.getPort()).isEqualTo(9090);
    }

    @Test
    void testShortPortOption(@TempDir Path tempDir) throws IOException {
        Path archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);
        String[] args = {"-p", "8888", "-a", archiveFile.toString()};
        CliConfig config = CliConfig.parse(args);

        assertThat(config.getPort()).isEqualTo(8888);
        assertThat(config.getArchivePath()).isEqualTo(archiveFile.toString());
    }

    @Test
    void testArchivePath(@TempDir Path tempDir) throws IOException {
        Path archiveFile = tempDir.resolve("archive.tar.gz");
        Files.createFile(archiveFile);
        String[] args = {"--archive", archiveFile.toString()};
        CliConfig config = CliConfig.parse(args);

        assertThat(config.getArchivePath()).isEqualTo(archiveFile.toString());
    }

    @Test
    void testMissingArchivePath(@TempDir Path tempDir) throws IOException {
        Path archiveFile = tempDir.resolve("archive.zip");
        Files.createFile(archiveFile);

        String[] args = {"--archive", archiveFile.toString()};
        CliConfig config = CliConfig.parse(args);

        assertThat(config.getArchivePath()).isEqualTo(archiveFile.toString());
    }

    @Test
    void testArchivePathRequired() {
        String[] args = {"--port", "8080"};

        assertThatThrownBy(() -> CliConfig.parse(args))
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("Archive path is required");
    }

    @Test
    void testArchiveFileMustExist(@TempDir Path tempDir) {
        String[] args = {"--archive", "/nonexistent/path/archive.zip"};

        assertThatThrownBy(() -> CliConfig.parse(args))
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void testArchiveFileMustBeReadable(@TempDir Path tempDir) throws IOException {
        Path archiveFile = tempDir.resolve("archive.zip");
        Files.createFile(archiveFile);
        Files.setPosixFilePermissions(archiveFile, java.nio.file.attribute.PosixFilePermissions.fromString("---------"));

        String[] args = {"--archive", archiveFile.toString()};

        assertThatThrownBy(() -> CliConfig.parse(args))
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("not readable");
    }

    @Test
    void testPortOutOfRangeLow(@TempDir Path tempDir) throws IOException {
        Path archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);
        String[] args = {"--port", "0", "--archive", archiveFile.toString()};

        assertThatThrownBy(() -> CliConfig.parse(args))
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("Port must be between");
    }

    @Test
    void testPortOutOfRangeHigh(@TempDir Path tempDir) throws IOException {
        Path archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);
        String[] args = {"--port", "65536", "--archive", archiveFile.toString()};

        assertThatThrownBy(() -> CliConfig.parse(args))
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("Port must be between");
    }

    @Test
    void testValidPortBoundaryMin(@TempDir Path tempDir) throws IOException {
        Path archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);
        String[] args = {"--port", "1", "--archive", archiveFile.toString()};
        CliConfig config = CliConfig.parse(args);

        assertThat(config.getPort()).isEqualTo(1);
    }

    @Test
    void testValidPortBoundaryMax(@TempDir Path tempDir) throws IOException {
        Path archiveFile = tempDir.resolve("test.zip");
        Files.createFile(archiveFile);
        String[] args = {"--port", "65535", "--archive", archiveFile.toString()};
        CliConfig config = CliConfig.parse(args);

        assertThat(config.getPort()).isEqualTo(65535);
    }

    @Test
    void testSupportsTarGzArchive(@TempDir Path tempDir) throws IOException {
        Path archiveFile = tempDir.resolve("archive.tar.gz");
        Files.createFile(archiveFile);

        String[] args = {"--archive", archiveFile.toString()};
        CliConfig config = CliConfig.parse(args);

        assertThat(config.getArchivePath()).endsWith(".tar.gz");
    }

    @Test
    void testSupportsXzArchive(@TempDir Path tempDir) throws IOException {
        Path archiveFile = tempDir.resolve("archive.tar.xz");
        Files.createFile(archiveFile);

        String[] args = {"--archive", archiveFile.toString()};
        CliConfig config = CliConfig.parse(args);

        assertThat(config.getArchivePath()).endsWith(".tar.xz");
    }
}
