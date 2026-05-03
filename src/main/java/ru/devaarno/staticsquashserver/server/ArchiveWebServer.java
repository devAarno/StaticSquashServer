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

import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRouting;
import ru.devaarno.staticsquashserver.archive.ArchiveDescriptor;
import ru.devaarno.staticsquashserver.archive.ArchiveEntryInfo;
import ru.devaarno.staticsquashserver.archive.ArchiveParser;
import ru.devaarno.staticsquashserver.archive.ArchiveParserFactory;
import ru.devaarno.staticsquashserver.cli.CliConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ArchiveWebServer {
    private final WebServer server;
    private final ArchiveParser archiveParser;
    private final ArchiveDescriptor archiveDescriptor;
    private final Path archivePath;
    private final RequestQueue requestQueue;

    public ArchiveWebServer(CliConfig config) {
        this.archivePath = Path.of(config.getArchivePath());
        
        if (!Files.exists(archivePath)) {
            throw new IllegalArgumentException("Archive file not found: " + archivePath);
        }
        
        this.archiveParser = ArchiveParserFactory.create(archivePath);
        this.archiveDescriptor = archiveParser.parse(archivePath);
        
        if (archiveDescriptor.entries().isEmpty()) {
            throw new IllegalStateException("Archive contains no files");
        }
        
        this.requestQueue = new RequestQueue(archiveParser, archivePath);
        
        this.server = WebServer.builder()
                .port(config.getPort())
                .routing(this::setupRouting)
                .build();
    }

    private void setupRouting(HttpRouting.Builder routing) {
        for (ArchiveEntryInfo entry : archiveDescriptor.entries()) {
            String path = "/" + normalizePath(entry.path());
            routing.get(path, createHandler(entry));
        }
        
        routing.any((req, resp) -> {
            resp.status(Status.NOT_FOUND_404);
            resp.send("Not found");
        });
    }

    private Handler createHandler(ArchiveEntryInfo entryInfo) {
        return (req, resp) -> {
            try {
                InputStream entryStream = requestQueue.getEntryStream(entryInfo.path());
                
                if (entryStream == null) {
                    resp.status(Status.NOT_FOUND_404);
                    resp.send("File not found in archive");
                    return;
                }
                
                resp.headers().contentLength(entryInfo.size());
                resp.headers().set(HeaderNames.CONTENT_TYPE, entryInfo.mediaType().text());
                try (InputStream is = entryStream) {
                    resp.outputStream().write(is.readAllBytes());
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                resp.status(Status.INTERNAL_SERVER_ERROR_500);
                resp.send("Request interrupted");
            }
        };
    }

    private String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    public void start() {
        server.start();
        System.out.println("Server started on port " + server.port());
    }

    public void awaitShutdown() {
        try {
            while (server.isRunning()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        server.stop();
        requestQueue.shutdown();
    }

    public int port() {
        return server.port();
    }

    public ArchiveDescriptor archiveDescriptor() {
        return archiveDescriptor;
    }

    private static final class RequestQueue {
        private final ConcurrentHashMap<String, EntryRequest> pendingRequests = new ConcurrentHashMap<>();
        private final ArchiveParser archiveParser;
        private final Path archivePath;
        private final ExecutorService executor;
        private volatile boolean shutdown = false;

        RequestQueue(ArchiveParser archiveParser, Path archivePath) {
            this.archiveParser = archiveParser;
            this.archivePath = archivePath;
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
        }

        InputStream getEntryStream(String entryPath) throws InterruptedException, IOException {
            if (shutdown) {
                throw new IOException("Request queue is shut down");
            }

            EntryRequest request = pendingRequests.computeIfAbsent(entryPath, path -> {
                EntryRequest req = new EntryRequest(path);
                executor.execute(() -> processRequest(req));
                return req;
            });

            request.latch().await();
            
            InputStream stream = request.result();
            if (stream == null) {
                throw new IOException("Entry not found: " + entryPath);
            }
            return stream;
        }

        private void processRequest(EntryRequest request) {
            try {
                InputStream stream = archiveParser.getEntryInputStream(archivePath, request.entryPath());
                request.setResult(stream);
            } catch (RuntimeException e) {
                request.setResult(null);
            } finally {
                request.latch().countDown();
                pendingRequests.remove(request.entryPath());
            }
        }

        void shutdown() {
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

        private static final class EntryRequest {
            private final String entryPath;
            private final java.util.concurrent.CountDownLatch latch;
            private volatile InputStream result;

            EntryRequest(String entryPath) {
                this.entryPath = entryPath;
                this.latch = new java.util.concurrent.CountDownLatch(1);
                this.result = null;
            }

            String entryPath() {
                return entryPath;
            }

            java.util.concurrent.CountDownLatch latch() {
                return latch;
            }

            InputStream result() {
                return result;
            }

            void setResult(InputStream result) {
                this.result = result;
            }
        }
    }
}
