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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.helidon.http.Status.INTERNAL_SERVER_ERROR_500;
import static io.helidon.http.Status.SERVICE_UNAVAILABLE_503;

public class ContentProvider {

    private static final Logger LOGGER = Logger.getLogger(ContentProvider.class.getName());

    private final ArchiveParser archiveParser;
    private final ArchiveDescriptor archiveDescriptor;
    private final ArrayBlockingQueue<RequestResponseTask> queue = new ArrayBlockingQueue<>(2000);
    private final AtomicBoolean isScanStarted = new AtomicBoolean(false);
    private final ReentrantLock scanLock = new ReentrantLock();

    public ContentProvider(final Path archivePath) {
        this.archiveParser = ArchiveParserFactory.create(archivePath);
        this.archiveDescriptor = archiveParser.initScan();
    }

    // This method works at Helidon virtual thread.
    public CompletableFuture<Boolean> fillOutput(final ServerRequest request, final ServerResponse response, final ArchiveEntryInfo archiveEntryInfo) {
        final var ret = new CompletableFuture<Boolean>();

        try {
            if (queue.offer(new RequestResponseTask(request, response, archiveEntryInfo, ret), 1, TimeUnit.SECONDS)) {
                tryStartScan();
            } else {
                response.status(SERVICE_UNAVAILABLE_503);
                response.send("Service temporarily unavailable.");
                ret.complete(false);
                LOGGER.log(Level.WARNING, "Server is overloaded.");
                return ret;
            }
        } catch (final InterruptedException e) {
            response.status(SERVICE_UNAVAILABLE_503);
            response.send("Service temporarily unavailable.");
            ret.complete(true);
            LOGGER.log(Level.SEVERE, "Interrupted", e);
            return ret;
        }

        return ret;
    }

    public List<ArchiveEntryInfo> getEntries() {
        return this.archiveDescriptor.entries();
    }

    private void tryStartScan() {
        if (isScanStarted.get()) {
            return;
        }

        if (scanLock.tryLock()) {
            try {
                if (isScanStarted.compareAndSet(false, true)) {
                    LOGGER.log(Level.INFO, "Archive scan loop has been started.");
                    Thread.ofPlatform().name("scan-thread").start(this::scan);
                }
            } finally {
                scanLock.unlock();
            }
        }
    }

    // This method works at real system thread.
    private void scan() {
        final var tenured = new ArrayList<RequestResponseTask>();
        try {
            while (!Thread.interrupted() && !(isScanStarted.compareAndSet(queue.isEmpty(), false))) {
                final var plan = new ArrayList<RequestResponseTask>();
                queue.drainTo(plan);

                LOGGER.log(Level.INFO, "The scan plan contains {0} entities.", plan.size());
                try {
                    archiveParser.forEachEntry((final String s, final InputStream inputStream) -> {
                        for (final var planItem : plan) {
                            if (planItem.archiveEntryInfo().path().equals(s)) {
                                planItem.response().headers().contentLength(planItem.archiveEntryInfo().size());
                                planItem.response().headers().set(HeaderNames.CONTENT_TYPE, planItem.archiveEntryInfo().mediaType().text());
                                try {
                                    inputStream.transferTo(planItem.response().outputStream());
                                } catch (final IOException e) {
                                    planItem.response().headers().set(HeaderNames.CONTENT_TYPE, 0);
                                    planItem.response().status(INTERNAL_SERVER_ERROR_500);
                                    LOGGER.log(Level.SEVERE, "Unable to transfer a data.", e);
                                }
                                planItem.isDone().complete(true);
                                LOGGER.log(Level.INFO, "Done: {0}", planItem.archiveEntryInfo().path());
                            }
                        }
                    });
                } catch (final IOException e) {
                    LOGGER.log(Level.SEVERE, "Archive is corrupted", e);
                    queue.forEach(
                            (final RequestResponseTask it) -> {
                                it.response().status(INTERNAL_SERVER_ERROR_500);
                                it.isDone().complete(true);
                            });
                    queue.clear();
                    return;
                }

                final var partitioned = plan
                        .stream()
                        .collect(
                                Collectors.partitioningBy(
                                        it -> it.isDone().isDone(),
                                        Collectors.toList()
                                )
                        );

                final var done = partitioned.get(true);
                final var undone = partitioned.get(false);

                tenured.removeAll(done);

                // This is means, that full scan loop unable to find a file.
                // Actually, this case is never expected due to a route-per-file concept at ArchiveWebServer::setupRouting.
                undone
                    .stream()
                    .filter(tenured::contains)
                    .forEach(
                            (final RequestResponseTask it) -> {
                                it.response().status(INTERNAL_SERVER_ERROR_500);
                                it.isDone().complete(true);
                                LOGGER.log(Level.SEVERE, "Unable to complete task: {0}", it);
                            }
                    );

                // We forget about stucked undone tasks
                undone.removeAll(tenured);

                // We add to tenured collection the final undone task collection.
                // It covers a case when loop is at the end of arhive but a file is required from its start.
                tenured.addAll(undone);

                queue.removeAll(done);
            }
        } finally {
            scanLock.lock();
            try {
                isScanStarted.set(false);
            } finally {
                scanLock.unlock();
            }
            LOGGER.log(Level.INFO, "Archive scan loop has been finished.");
        }
    }
}
