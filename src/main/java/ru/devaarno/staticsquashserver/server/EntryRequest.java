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

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

/**
 * Represents a pending request for an archive entry.
 * Used internally by RequestQueue to track and synchronize concurrent requests.
 * Uses a piped stream to avoid buffering data in memory.
 */
public final class EntryRequest {
    private final String entryPath;
    private final CountDownLatch latch;
    private final PipedInputStream inputStream;
    private final PipedOutputStream outputStream;
    private volatile Throwable error;

    public EntryRequest(String entryPath) throws IOException {
        this.entryPath = entryPath;
        this.latch = new CountDownLatch(1);
        this.inputStream = new PipedInputStream();
        this.outputStream = new PipedOutputStream(inputStream);
        this.error = null;
    }

    public String entryPath() {
        return entryPath;
    }

    public CountDownLatch latch() {
        return latch;
    }

    public PipedInputStream inputStream() {
        return inputStream;
    }

    public PipedOutputStream outputStream() {
        return outputStream;
    }

    public void setResult(Throwable error) {
        this.error = error;
    }

    public Throwable error() {
        return error;
    }
}
