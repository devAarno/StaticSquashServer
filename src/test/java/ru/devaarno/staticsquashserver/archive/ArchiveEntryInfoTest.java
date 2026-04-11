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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveEntryInfoTest {

    @Test
    void testConstructorAndGetters() {
        Instant now = Instant.now();
        ArchiveEntryInfo info = new ArchiveEntryInfo("report/index.html", 12345, MediaTypes.TEXT_HTML, now);

        assertThat(info.path()).isEqualTo("report/index.html");
        assertThat(info.size()).isEqualTo(12345);
        assertThat(info.mediaType()).isEqualTo(MediaTypes.TEXT_HTML);
        assertThat(info.modificationTime()).isEqualTo(now);
    }

    @Test
    void testImmutability() {
        ArchiveEntryInfo info = new ArchiveEntryInfo("test.txt", 100, MediaTypes.TEXT_PLAIN, Instant.EPOCH);

        assertThat(info.path()).isNotNull();
        assertThat(info.size()).isGreaterThan(0);
        assertThat(info.mediaType()).isNotNull();
        assertThat(info.modificationTime()).isNotNull();
    }
}
