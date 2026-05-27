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

import io.helidon.http.Status;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import ru.devaarno.staticsquashserver.archive.ArchiveEntryInfo;
import ru.devaarno.staticsquashserver.cli.CliConfig;
import ru.devaarno.staticsquashserver.contentprovider.ContentProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ArchiveWebServer {

    private static final Logger LOGGER = Logger.getLogger(ArchiveWebServer.class.getName());

    private final WebServer server;
    private final Path archivePath;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private final ContentProvider contentProvider;

    public ArchiveWebServer(final CliConfig config) {
        this.archivePath = Path.of(config.getArchivePath());
        
        if (!Files.exists(archivePath)) {
            throw new IllegalArgumentException("Archive file not found: " + archivePath);
        }

        LOGGER.log(Level.INFO, "Parsing archive: {0}", archivePath);

        this.contentProvider = new ContentProvider(this.archivePath);
        
        this.server = WebServer
                .builder()
                .port(config.getPort())
                .routing(this::setupRouting)
                .build();
    }

    private void setupRouting(final HttpRouting.Builder routing) {
        for (final ArchiveEntryInfo entry : this.contentProvider.getEntries()) {
            String path = "/" + normalizePath(entry.path());
            routing.get(path, createHandler(entry));
        }
        
        routing.any((final ServerRequest _, final ServerResponse resp) -> {
            resp.status(Status.NOT_FOUND_404);
            resp.send("Not found");
        });
    }

    private Handler createHandler(final ArchiveEntryInfo entryInfo) {
        return (final ServerRequest request, final ServerResponse response) -> {
            LOGGER.log(Level.INFO, "Request: {0}", entryInfo.path());
            contentProvider.fillOutput(request, response, entryInfo).get(5, TimeUnit.MINUTES);
            /*try {
                resp.headers().contentLength(entryInfo.size());
                resp.headers().set(HeaderNames.CONTENT_TYPE, entryInfo.mediaType().text());
                this.archiveParser.fillOutput(entryInfo.path(), resp.outputStream());
            } catch (final FileNotFoundException e) {
                resp.status(Status.NOT_FOUND_404);
                resp.send("File not found: " + e.getMessage());
            } catch (final IOException e) {
                resp.status(Status.INTERNAL_SERVER_ERROR_500);
                resp.send("Error: " + e.getMessage());
            }*/

        };
    }

    private String normalizePath(final String path) {
        return path.replace('\\', '/');
    }

    public void start() {
        server.start();
        LOGGER.log(Level.INFO, "Server started on port {0}", server.port());
    }

    public void awaitShutdown() {
        try {
            shutdownLatch.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        LOGGER.info("Stopping server...");
        server.stop();
        shutdownLatch.countDown();
        LOGGER.info("Server stopped");
    }

    public int port() {
        return server.port();
    }
}
