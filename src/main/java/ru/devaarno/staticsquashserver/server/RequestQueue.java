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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

import ru.devaarno.staticsquashserver.archive.ArchiveParser;

/**
 * Queue for managing concurrent requests to archive entries.
 * Merges duplicate requests for the same entry and processes them using virtual threads.
 * Each waiting request receives its own copy of the entry data.
 */
public final class RequestQueue {
    private final ConcurrentHashMap<String, EntryRequest> pendingRequests = new ConcurrentHashMap<>();
    private final ArchiveParser archiveParser;
    private final Path archivePath;
    private final ExecutorService executor;
    private volatile boolean shutdown = false;

    public RequestQueue(ArchiveParser archiveParser, Path archivePath) {
        this.archiveParser = archiveParser;
        this.archivePath = archivePath;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Gets an input stream for an archive entry.
     * If multiple requests for the same entry are pending, they are merged and wait for the same result.
     * Each caller receives its own independent InputStream.
     *
     * @param entryPath path of the entry in the archive
     * @return input stream for the entry content
     * @throws IOException if entry not found or queue is shut down
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public InputStream getEntryStream(String entryPath) throws IOException, InterruptedException {
        if (shutdown) {
            throw new IOException("Request queue is shut down");
        }

        EntryRequest request = pendingRequests.computeIfAbsent(entryPath, path -> {
            EntryRequest req = new EntryRequest(path);
            executor.execute(() -> processRequest(req));
            return req;
        });

        request.latch().await();

        byte[] data = request.result();
        if (data == null) {
            throw new IOException("Entry not found: " + entryPath);
        }
        return new ByteArrayInputStream(data);
    }

    private void processRequest(EntryRequest request) {
        InputStream stream = archiveParser.getEntryInputStream(archivePath, request.entryPath());
        try {
            if (stream == null) {
                request.setResult(null);
            } else {
                byte[] data = stream.readAllBytes();
                request.setResult(data);
            }
        } catch (IOException e) {
            request.setResult(null);
        } finally {
            request.latch().countDown();
            pendingRequests.remove(request.entryPath());
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore close exception
                }
            }
        }
    }

    /**
     * Shuts down the request queue gracefully.
     */
    public void shutdown() {
        shutdown = true;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
