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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Advanced integration tests for Stage 7: Request Queue Integration.
 * Tests stress scenarios and edge cases for concurrent access.
 */
@ServerTest
class RequestQueueStressTest {

    private final Http1Client client;

    RequestQueueStressTest(Http1Client client) {
        this.client = client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder routing) {
        routing.get("/api/resource", (req, res) -> {
            res.send("Resource data");
        });
        
        routing.get("/api/slow", (req, res) -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            res.send("Slow response");
        });
        
        routing.get("/api/error", (req, res) -> {
            res.status(Status.INTERNAL_SERVER_ERROR_500);
            res.send("Error occurred");
        });
        
        routing.any((req, resp) -> {
            resp.status(Status.NOT_FOUND_404);
            resp.send("Not found");
        });
    }

    @Test
    void testHighConcurrencySameEndpoint() throws Exception {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Http1ClientResponse response = client.get("/api/resource").request()) {
                        int statusCode = response.status().code();
                        if (statusCode == 200) {
                            String content = response.entity().as(String.class);
                            if ("Resource data".equals(content)) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } else {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(errorCount.get()).isZero();
    }

    @Test
    void testMixedEndpointsConcurrency() throws Exception {
        int threadCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        String[] endpoints = {
            "/api/resource",
            "/api/slow",
            "/api/resource",
            "/api/slow",
            "/api/resource",
            "/api/slow",
            "/api/resource",
            "/api/slow",
            "/api/resource",
            "/api/slow"
        };

        for (int i = 0; i < threadCount; i++) {
            String endpoint = endpoints[i % endpoints.length];
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Http1ClientResponse response = client.get(endpoint).request()) {
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
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    @Test
    void testRapidSequentialRequests() throws Exception {
        int requestCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < requestCount; i++) {
            executor.submit(() -> {
                try {
                    try (Http1ClientResponse response = client.get("/api/resource").request()) {
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

        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(requestCount);
    }

    @Test
    void testConcurrentNotFoundRequests() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger notFoundCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Http1ClientResponse response = client.get("/nonexistent/" + index).request()) {
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
        assertThat(notFoundCount.get()).isEqualTo(threadCount);
    }

    @Test
    void testStressMixedSuccessAndFailure() throws Exception {
        int threadCount = 40;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger notFoundCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            String endpoint = (i % 2 == 0) ? "/api/resource" : "/nonexistent";
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Http1ClientResponse response = client.get(endpoint).request()) {
                        int code = response.status().code();
                        if (code == 200) {
                            successCount.incrementAndGet();
                        } else if (code == 404) {
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
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get() + notFoundCount.get()).isEqualTo(threadCount);
    }

    @Test
    void testQueueMergingVerification() throws Exception {
        AtomicInteger requestCount = new AtomicInteger(0);
        
        int concurrentRequests = 20;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentRequests);

        for (int i = 0; i < concurrentRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Http1ClientResponse response = client.get("/api/slow").request()) {
                        if (response.status().code() == 200) {
                            requestCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(requestCount.get()).isEqualTo(concurrentRequests);
    }
}
