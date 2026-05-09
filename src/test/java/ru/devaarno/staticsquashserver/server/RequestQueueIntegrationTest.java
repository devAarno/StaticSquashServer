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
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Stage 7: Request Queue Integration.
 * Tests concurrent access to the same resource and queue merging behavior.
 */
@ServerTest
class RequestQueueIntegrationTest {

    private final Http1Client client;

    RequestQueueIntegrationTest(Http1Client client) {
        this.client = client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder routing) {
        routing.get("/static/file1.txt", (req, res) -> {
            res.send("Content of file 1");
        });
        
        routing.get("/static/file2.txt", (req, res) -> {
            res.send("Content of file 2");
        });
        
        routing.get("/static/large-file.bin", (req, res) -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("Line ").append(i).append("\n");
            }
            res.send(sb.toString());
        });
        
        routing.any((req, resp) -> {
            resp.status(Status.NOT_FOUND_404);
            resp.send("Not found");
        });
    }

    @Test
    void testConcurrentRequestsSameFile() throws Exception {
        int concurrentRequests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger responseCodeSum = new AtomicInteger(0);

        for (int i = 0; i < concurrentRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Http1ClientResponse response = client.get("/static/file1.txt").request()) {
                        int statusCode = response.status().code();
                        responseCodeSum.addAndGet(statusCode);
                        
                        if (statusCode == 200) {
                            String content = response.entity().as(String.class);
                            if ("Content of file 1".equals(content)) {
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
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(concurrentRequests);
        assertThat(responseCodeSum.get()).isEqualTo(concurrentRequests * 200);
    }

    @Test
    void testConcurrentRequestsDifferentFiles() throws Exception {
        int concurrentRequests = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);

        String[] paths = {
            "/static/file1.txt",
            "/static/file2.txt",
            "/static/file1.txt",
            "/static/file2.txt",
            "/static/large-file.bin"
        };

        for (int i = 0; i < concurrentRequests; i++) {
            String path = paths[i];
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Http1ClientResponse response = client.get(path).request()) {
                        if (response.status().code() == 200) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(concurrentRequests);
    }

    @Test
    void testSequentialRequestsSameFile() {
        for (int i = 0; i < 5; i++) {
            try (Http1ClientResponse response = client.get("/static/file1.txt").request()) {
                assertThat(response.status().code()).isEqualTo(200);
                String content = response.entity().as(String.class);
                assertThat(content).isEqualTo("Content of file 1");
            }
        }
    }

    @Test
    void testLargeFileConcurrentAccess() throws Exception {
        int concurrentRequests = 3;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < concurrentRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Http1ClientResponse response = client.get("/static/large-file.bin").request()) {
                        if (response.status().code() == 200) {
                            String content = response.entity().as(String.class);
                            if (content.contains("Line 0") && content.contains("Line 999")) {
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
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(concurrentRequests);
    }

    @Test
    void testNotFoundWithConcurrentRequests() throws Exception {
        int concurrentRequests = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger notFoundCount = new AtomicInteger(0);

        for (int i = 0; i < concurrentRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Http1ClientResponse response = client.get("/nonexistent.txt").request()) {
                        if (response.status().code() == 404) {
                            notFoundCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(notFoundCount.get()).isEqualTo(concurrentRequests);
    }
}
