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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestQueueTest {

    @Test
    void testSingleRequest() throws Exception {
        MockArchiveParser parser = new MockArchiveParser("test.txt", "Hello World");
        RequestQueue queue = new RequestQueue(parser, Path.of("/fake/archive.zip"));

        try (InputStream stream = queue.getEntryStream("test.txt")) {
            assertThat(stream).isNotNull();
            String content = new String(stream.readAllBytes());
            assertThat(content).isEqualTo("Hello World");
        } finally {
            queue.shutdown();
        }
    }

    @Test
    void testMultipleRequestsSameFile() throws Exception {
        MockArchiveParser parser = new MockArchiveParser("data.json", "{\"key\":\"value\"}");
        RequestQueue queue = new RequestQueue(parser, Path.of("/fake/archive.zip"));

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (InputStream stream = queue.getEntryStream("data.json")) {
                        if (stream != null) {
                            String content = new String(stream.readAllBytes());
                            if ("{\"key\":\"value\"}".equals(content)) {
                                successCount.incrementAndGet();
                            }
                        }
                    }
                } catch (Exception e) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(threadCount);
        queue.shutdown();
    }

    @Test
    void testMultipleRequestsDifferentFiles() throws Exception {
        MockArchiveParser parser = new MockArchiveParser(
                "file1.txt", "Content 1",
                "file2.txt", "Content 2",
                "file3.txt", "Content 3"
        );
        RequestQueue queue = new RequestQueue(parser, Path.of("/fake/archive.zip"));

        try (InputStream stream1 = queue.getEntryStream("file1.txt");
             InputStream stream2 = queue.getEntryStream("file2.txt");
             InputStream stream3 = queue.getEntryStream("file3.txt")) {

            assertThat(new String(stream1.readAllBytes())).isEqualTo("Content 1");
            assertThat(new String(stream2.readAllBytes())).isEqualTo("Content 2");
            assertThat(new String(stream3.readAllBytes())).isEqualTo("Content 3");
        } finally {
            queue.shutdown();
        }
    }

    @Test
    void testNonExistentFile() throws Exception {
        MockArchiveParser parser = new MockArchiveParser("existing.txt", "Exists");
        RequestQueue queue = new RequestQueue(parser, Path.of("/fake/archive.zip"));

        assertThatThrownBy(() -> queue.getEntryStream("nonexistent.txt"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Entry not found");

        queue.shutdown();
    }

    @Test
    void testShutdownDuringRequest() throws Exception {
        SlowArchiveParser parser = new SlowArchiveParser();
        RequestQueue queue = new RequestQueue(parser, Path.of("/fake/archive.zip"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch requestStarted = new CountDownLatch(1);
        CountDownLatch requestDone = new CountDownLatch(1);

        executor.submit(() -> {
            try {
                requestStarted.countDown();
                queue.getEntryStream("slow.txt");
            } catch (Exception e) {
            } finally {
                requestDone.countDown();
            }
        });

        requestStarted.await(5, TimeUnit.SECONDS);
        Thread.sleep(100);
        queue.shutdown();
        
        requestDone.await(5, TimeUnit.SECONDS);
        executor.shutdown();
    }

    static class MockArchiveParser implements ru.devaarno.staticsquashserver.archive.ArchiveParser {
        private final java.util.Map<String, String> entries;

        MockArchiveParser(String... entries) {
            this.entries = new java.util.HashMap<>();
            for (int i = 0; i < entries.length; i += 2) {
                this.entries.put(entries[i], entries[i + 1]);
            }
        }

        @Override
        public ru.devaarno.staticsquashserver.archive.ArchiveDescriptor parse(Path archivePath) {
            return null;
        }

        @Override
        public InputStream getEntryInputStream(Path archivePath, String entryPath) {
            String content = entries.get(entryPath);
            return content != null ? new ByteArrayInputStream(content.getBytes()) : null;
        }

        @Override
        public ru.devaarno.staticsquashserver.archive.ArchiveFormat format() {
            return ru.devaarno.staticsquashserver.archive.ArchiveFormat.ZIP;
        }

        @Override
        public void forEachEntry(Path archivePath, java.util.function.Consumer<ru.devaarno.staticsquashserver.archive.ArchiveEntryInfo> entryConsumer) {
        }
    }

    static class SlowArchiveParser implements ru.devaarno.staticsquashserver.archive.ArchiveParser {
        @Override
        public ru.devaarno.staticsquashserver.archive.ArchiveDescriptor parse(Path archivePath) {
            return null;
        }

        @Override
        public InputStream getEntryInputStream(Path archivePath, String entryPath) {
            try {
                Thread.sleep(10000);
                return new ByteArrayInputStream("Slow content".getBytes());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        @Override
        public ru.devaarno.staticsquashserver.archive.ArchiveFormat format() {
            return ru.devaarno.staticsquashserver.archive.ArchiveFormat.ZIP;
        }

        @Override
        public void forEachEntry(Path archivePath, java.util.function.Consumer<ru.devaarno.staticsquashserver.archive.ArchiveEntryInfo> entryConsumer) {
        }
    }
}
