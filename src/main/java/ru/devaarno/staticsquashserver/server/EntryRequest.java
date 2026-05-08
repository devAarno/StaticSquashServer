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

package ru.devaarno.staticsquashserver.server;

import java.util.concurrent.CountDownLatch;

/**
 * Represents a pending request for an archive entry.
 * Used internally by RequestQueue to track and synchronize concurrent requests.
 */
public final class EntryRequest {
    private final String entryPath;
    private final CountDownLatch latch;
    private volatile byte[] result;

    public EntryRequest(String entryPath) {
        this.entryPath = entryPath;
        this.latch = new CountDownLatch(1);
        this.result = null;
    }

    public String entryPath() {
        return entryPath;
    }

    public CountDownLatch latch() {
        return latch;
    }

    public byte[] result() {
        return result;
    }

    public void setResult(byte[] result) {
        this.result = result;
    }
}
