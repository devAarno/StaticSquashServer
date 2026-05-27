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

package ru.devaarno.staticsquashserver.contentprovider;

import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import ru.devaarno.staticsquashserver.archive.ArchiveDescriptor;
import ru.devaarno.staticsquashserver.archive.ArchiveEntryInfo;
import ru.devaarno.staticsquashserver.archive.ArchiveParser;
import ru.devaarno.staticsquashserver.archive.ArchiveParserFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContentProvider {

    private static final Logger LOGGER = Logger.getLogger(ContentProvider.class.getName());

    private final ArchiveParser archiveParser;
    private final ArchiveDescriptor archiveDescriptor;
    private final ArrayBlockingQueue<RequestResponseTask> queue = new ArrayBlockingQueue<>(2000);
    private final AtomicBoolean isLoopStarted = new AtomicBoolean(false);

    public ContentProvider(final Path archivePath) {
        this.archiveParser = ArchiveParserFactory.create(archivePath);
        this.archiveDescriptor = archiveParser.initScan();
    }

    public CompletableFuture<Boolean> fillOutput(final ServerRequest request, final ServerResponse response, final ArchiveEntryInfo archiveEntryInfo) {
        final var ret = new CompletableFuture<Boolean>();

        try {
            queue.offer(new RequestResponseTask(request, response, archiveEntryInfo, ret), 1, TimeUnit.SECONDS);
            startScan();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

        return ret;


        /*try {
            requestResponse.response().headers().contentLength(archiveEntryInfo.size());
            requestResponse.response().headers().set(HeaderNames.CONTENT_TYPE, archiveEntryInfo.mediaType().text());

            this.archiveParser.fillOutput(archiveEntryInfo.path(), requestResponse.response().outputStream());
        } catch (final FileNotFoundException e) {
            requestResponse.response().status(Status.NOT_FOUND_404);
            requestResponse.response().send("File not found: " + e.getMessage());
        } catch (final IOException e) {
            requestResponse.response().status(Status.INTERNAL_SERVER_ERROR_500);
            requestResponse.response().send("Error: " + e.getMessage());
        }*/
    }

    public List<ArchiveEntryInfo> getEntries() {
        return this.archiveDescriptor.entries();
    }

    private void startScan() {
        if (isLoopStarted.compareAndSet(false, true)) {
            LOGGER.log(Level.INFO, "Archive scan loop has been started.");
            try (final var executor = Executors.newSingleThreadExecutor()) {
                executor.submit(() -> {
                    while (!queue.isEmpty()) {
                        final var plan = new ArrayList<RequestResponseTask>();
                        queue.drainTo(plan);

                        LOGGER.log(Level.INFO, "The scan plan contains {0} entities.", plan.size());
                        try {
                            archiveParser.forEachEntry((s, inputStream) -> {
                                for (final var planItem : plan) {
                                    if (planItem.archiveEntryInfo().path().equals(s)) {
                                        planItem.response().headers().contentLength(planItem.archiveEntryInfo().size());
                                        planItem.response().headers().set(HeaderNames.CONTENT_TYPE, planItem.archiveEntryInfo().mediaType().text());
                                        try {
                                            inputStream.transferTo(planItem.response().outputStream());
                                            planItem.isDone().complete(true);
                                            LOGGER.log(Level.INFO, "Done: {0}", planItem.archiveEntryInfo().path());
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                            });
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        queue.removeAll(plan);
                    }
                }).get();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                isLoopStarted.set(false);
                LOGGER.log(Level.INFO, "Archive scan loop has been finished.");
            }
        }

    }
}
