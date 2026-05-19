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

import ru.devaarno.staticsquashserver.archive.ArchiveParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Queue for managing concurrent requests to archive entries.
 * Merges duplicate requests for the same entry and processes them using virtual threads.
 * Uses piped streams to avoid buffering data in memory.
 */
public final class RequestQueue {

    private static final Logger LOGGER = Logger.getLogger(RequestQueue.class.getName());

    private final ConcurrentHashMap<String, EntryRequest> pendingRequests = new ConcurrentHashMap<>();
    private final ArchiveParser archiveParser;
    private final Path archivePath;
    private final ExecutorService executor;
    private volatile boolean shutdown = false;

    public RequestQueue(ArchiveParser archiveParser, Path archivePath) {
        this.archiveParser = archiveParser;
        this.archivePath = archivePath;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        if (archivePath != null) {
            LOGGER.info("RequestQueue initialized for archive: " + archivePath.getFileName());
        } else {
            LOGGER.info("RequestQueue initialized (test mode)");
        }
    }

    /**
     * Gets an input stream for an archive entry.
     * If multiple requests for the same entry are pending, they are merged and wait for the same result.
     * The stream is provided directly from the archive without buffering.
     *
     * @param entryPath path of the entry in the archive
     * @return input stream for the entry content
     * @throws IOException if entry not found or queue is shut down
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public InputStream getEntryStream(String entryPath) throws IOException, InterruptedException {
        if (shutdown) {
            LOGGER.log(Level.WARNING, "Request queue is shut down, rejecting request for: {0}", entryPath);
            throw new IOException("Request queue is shut down");
        }

        boolean isNew = !pendingRequests.containsKey(entryPath);
        EntryRequest request = pendingRequests.computeIfAbsent(entryPath, path -> {
            EntryRequest req;
            try {
                req = new EntryRequest(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create entry request", e);
            }
            LOGGER.log(Level.INFO, "Processing entry: {0}", path);
            final EntryRequest finalReq = req;
            executor.execute(() -> processRequest(finalReq));
            return req;
        });

        if (!isNew) {
            LOGGER.log(Level.INFO, "Merged duplicate request for: {0}", entryPath);
        }

        request.latch().await();

        if (request.error() != null) {
            LOGGER.log(Level.WARNING, "Entry processing failed: {0}: {1}", new Object[]{entryPath, request.error().getMessage()});
            throw new IOException("Entry processing failed: " + entryPath, request.error());
        }

        return request.inputStream();
    }

    private void processRequest(EntryRequest request) {
        InputStream stream = null;
        OutputStream outputStream = null;
        try {
            stream = archiveParser.getEntryInputStream(archivePath, request.entryPath());
            if (stream == null) {
                request.setResult(new IOException("Entry not found"));
            } else {
                stream.transferTo(request.outputStream());
            }
        } catch (final IOException e) {
            if (request.error() == null) {
                request.setResult(e);
            }
        } finally {
            /* if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (final IOException e) {
                    LOGGER.log(Level.FINE, "Error closing output stream", e);
                }
            }*/
            if (stream != null) {
                try {
                    stream.close();
                } catch (final IOException e) {
                    LOGGER.log(Level.FINE, "Error closing input stream", e);
                }
            }
            request.latch().countDown();
            pendingRequests.remove(request.entryPath());
        }
    }

    /**
     * Shuts down the request queue gracefully.
     */
    public void shutdown() {
        LOGGER.info("Shutting down RequestQueue...");
        shutdown = true;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warning("Executor termination timeout, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            LOGGER.warning("Interrupted during shutdown, forcing executor shutdown");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("RequestQueue shutdown complete");
    }
}
