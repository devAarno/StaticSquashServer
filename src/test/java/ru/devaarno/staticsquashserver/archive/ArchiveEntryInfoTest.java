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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveEntryInfoTest {

    @Test
    void testConstructorAndGetters() {
        Instant now = Instant.now();
        ArchiveEntryInfo info = new ArchiveEntryInfo("report/index.html", 12345, MediaTypes.TEXT_HTML, now);

        assertEquals("report/index.html", info.path(), "Path should match");
        assertEquals(12345, info.size(), "Size should match");
        assertEquals(MediaTypes.TEXT_HTML, info.mediaType(), "MediaType should match");
        assertEquals(now, info.modificationTime(), "Modification time should match");
    }

    @Test
    void testImmutability() {
        ArchiveEntryInfo info = new ArchiveEntryInfo("test.txt", 100, MediaTypes.TEXT_PLAIN, Instant.EPOCH);

        assertNotNull(info.path(), "Path should not be null");
        assertTrue(info.size() > 0, "Size should be greater than 0");
        assertNotNull(info.mediaType(), "MediaType should not be null");
        assertNotNull(info.modificationTime(), "Modification time should not be null");
    }
}
