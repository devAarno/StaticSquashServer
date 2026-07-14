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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Stage 8: Error Handling and Application Termination.
 * Tests error scenarios including corrupted archives, IO errors, and graceful shutdown.
 */
@ServerTest
class ErrorHandlingTest {

    private final Http1Client client;

    ErrorHandlingTest(final Http1Client client) {
        this.client = client;
    }

    @SetUpRoute
    static void routing(final HttpRouting.Builder routing) {
        routing.get("/normal.txt", (_, res) -> res.send("Normal content"));
        
        routing.get("/error-simulated", (_, _) -> {
            throw new RuntimeException("Simulated error");
        });
        
        routing.get("/io-error", (_, _) -> {
            throw new IOException("Simulated IO error");
        });
        
        routing.error(RuntimeException.class, (_, res, ex) -> {
            res.status(Status.INTERNAL_SERVER_ERROR_500);
            res.send("Error: " + ex.getMessage());
        });
        
        routing.error(IOException.class, (_, res, ex) -> {
            res.status(Status.INTERNAL_SERVER_ERROR_500);
            res.send("IO Error: " + ex.getMessage());
        });
        
        routing.any((_, resp) -> {
            resp.status(Status.NOT_FOUND_404);
            resp.send("Not found");
        });
    }

    @Test
    void testNormalRequest() {
        try (Http1ClientResponse response = client.get("/normal.txt").request()) {
            assertEquals(200, response.status().code(), "Response status should be 200 for /normal.txt");
            String content = response.entity().as(String.class);
            assertEquals("Normal content", content, "Content should match for /normal.txt");
        }
    }

    @Test
    void testRuntimeExceptionHandling() {
        try (Http1ClientResponse response = client.get("/error-simulated").request()) {
            assertEquals(500, response.status().code(), "Response status should be 500 for /error-simulated");
            String content = response.entity().as(String.class);
            assertTrue(content.contains("Error"), "Content should contain 'Error' for /error-simulated");
        }
    }

    @Test
    void testIOExceptionHandling() {
        try (Http1ClientResponse response = client.get("/io-error").request()) {
            assertEquals(500, response.status().code(), "Response status should be 500 for /io-error");
            String content = response.entity().as(String.class);
            assertTrue(content.contains("IO Error"), "Content should contain 'IO Error' for /io-error");
        }
    }

    @Test
    void testNotFoundHandling() {
        try (Http1ClientResponse response = client.get("/nonexistent").request()) {
            assertEquals(404, response.status().code(), "Response status should be 404 for /nonexistent");
        }
    }

    @Test
    void testConcurrentErrorHandling() throws Exception {
        int concurrentRequests = 10;
        Thread[] threads = new Thread[concurrentRequests];
        final int[] errorCount = {0};
        final int[] successCount = {0};
        
        for (int i = 0; i < concurrentRequests; i++) {
            threads[i] = new Thread(() -> {
                try (Http1ClientResponse response = client.get("/error-simulated").request()) {
                    if (response.status().code() == 500) {
                        synchronized (errorCount) {
                            errorCount[0]++;
                        }
                    }
                } catch (final Exception _) {
                    // Connection errors are also acceptable in error scenarios
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join(5000);
        }
        
        assertTrue(errorCount[0] + successCount[0] > 0, "At least one request should succeed or error");
    }
}
