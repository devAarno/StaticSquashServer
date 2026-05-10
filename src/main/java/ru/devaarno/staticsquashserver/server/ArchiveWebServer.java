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
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import ru.devaarno.staticsquashserver.archive.ArchiveDescriptor;
import ru.devaarno.staticsquashserver.archive.ArchiveEntryInfo;
import ru.devaarno.staticsquashserver.archive.ArchiveParser;
import ru.devaarno.staticsquashserver.archive.ArchiveParserFactory;
import ru.devaarno.staticsquashserver.cli.CliConfig;
import ru.devaarno.staticsquashserver.logging.SimpleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ArchiveWebServer {
    private final WebServer server;
    private final ArchiveParser archiveParser;
    private final ArchiveDescriptor archiveDescriptor;
    private final Path archivePath;
    private final RequestQueue requestQueue;

    public ArchiveWebServer(final CliConfig config) {
        this.archivePath = Path.of(config.getArchivePath());
        
        if (!Files.exists(archivePath)) {
            throw new IllegalArgumentException("Archive file not found: " + archivePath);
        }
        
        SimpleLogger.info("Parsing archive: " + archivePath);
        this.archiveParser = ArchiveParserFactory.create(archivePath);
        this.archiveDescriptor = archiveParser.parse(archivePath);
        
        if (archiveDescriptor.entries().isEmpty()) {
            throw new IllegalStateException("Archive contains no files");
        }
        
        SimpleLogger.info("Archive parsed successfully: " + archiveDescriptor.entries().size() + " files found");
        this.requestQueue = new RequestQueue(archiveParser, archivePath);
        
        this.server = WebServer
                .builder()
                .port(config.getPort())
                .routing(this::setupRouting)
                .build();
    }

    private void setupRouting(final HttpRouting.Builder routing) {
        for (final ArchiveEntryInfo entry : archiveDescriptor.entries()) {
            String path = "/" + normalizePath(entry.path());
            routing.get(path, createHandler(entry));
        }
        
        routing.any((final ServerRequest _, final ServerResponse resp) -> {
            resp.status(Status.NOT_FOUND_404);
            resp.send("Not found");
        });
    }

    private Handler createHandler(ArchiveEntryInfo entryInfo) {
        return (final ServerRequest _, final ServerResponse resp) -> {
            try {
                InputStream entryStream = requestQueue.getEntryStream(entryInfo.path());
                
                if (entryStream == null) { // Check it
                    resp.status(Status.NOT_FOUND_404);
                    resp.send("File not found in archive");
                    return;
                }
                
                resp.headers().contentLength(entryInfo.size());
                resp.headers().set(HeaderNames.CONTENT_TYPE, entryInfo.mediaType().text());
                try (
                        final var inputStream = entryStream;
                        final var outputStream = resp.outputStream()
                ) {
                    inputStream.transferTo(outputStream);
                }
                
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                resp.status(Status.INTERNAL_SERVER_ERROR_500);
                resp.send("Request interrupted");
            } catch (final IOException e) {
                resp.status(Status.INTERNAL_SERVER_ERROR_500);
                resp.send("Error: " + e.getMessage());
            }
        };
    }

    private String normalizePath(final String path) {
        return path.replace('\\', '/');
    }

    public void start() {
        server.start();
        SimpleLogger.info("Server started on port " + server.port());
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
        SimpleLogger.info("Stopping server...");
        server.stop();
        requestQueue.shutdown();
        SimpleLogger.info("Server stopped");
    }

    public int port() {
        return server.port();
    }

    public ArchiveDescriptor archiveDescriptor() {
        return archiveDescriptor;
    }
}
