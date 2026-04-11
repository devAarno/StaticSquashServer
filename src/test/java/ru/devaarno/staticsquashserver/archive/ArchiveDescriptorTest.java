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

import io.helidon.common.media.type.MediaTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveDescriptorTest {

    @Test
    void testConstructorAndGetters(@TempDir Path tempDir) {
        Path archivePath = tempDir.resolve("test.zip");
        ArchiveEntryInfo entry = new ArchiveEntryInfo("file.txt", 100, MediaTypes.TEXT_PLAIN, Instant.EPOCH);
        List<ArchiveEntryInfo> entries = List.of(entry);

        ArchiveDescriptor descriptor = new ArchiveDescriptor(archivePath, ArchiveFormat.ZIP, entries);

        assertThat(descriptor.archivePath()).isEqualTo(archivePath);
        assertThat(descriptor.format()).isEqualTo(ArchiveFormat.ZIP);
        assertThat(descriptor.entries()).hasSize(1);
        assertThat(descriptor.entries().getFirst()).isEqualTo(entry);
    }

    @Test
    void testEmptyEntries(@TempDir Path tempDir) {
        Path archivePath = tempDir.resolve("empty.tar.gz");
        ArchiveDescriptor descriptor = new ArchiveDescriptor(archivePath, ArchiveFormat.TAR_GZ, List.of());

        assertThat(descriptor.entries()).isEmpty();
    }

    @Test
    void testArchiveFormatValues() {
        assertThat(ArchiveFormat.ZIP).isNotNull();
        assertThat(ArchiveFormat.TAR_GZ).isNotNull();
        assertThat(ArchiveFormat.TAR_XZ).isNotNull();
    }
}
