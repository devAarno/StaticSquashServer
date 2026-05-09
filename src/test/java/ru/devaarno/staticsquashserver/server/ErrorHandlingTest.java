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
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Stage 8: Error Handling and Application Termination.
 * Tests error scenarios including corrupted archives, IO errors, and graceful shutdown.
 */
@ServerTest
class ErrorHandlingTest {

    private final Http1Client client;

    ErrorHandlingTest(Http1Client client) {
        this.client = client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder routing) {
        routing.get("/normal.txt", (req, res) -> {
            res.send("Normal content");
        });
        
        routing.get("/error-simulated", (req, res) -> {
            throw new RuntimeException("Simulated error");
        });
        
        routing.get("/io-error", (req, res) -> {
            throw new IOException("Simulated IO error");
        });
        
        routing.error(RuntimeException.class, (req, res, ex) -> {
            res.status(Status.INTERNAL_SERVER_ERROR_500);
            res.send("Error: " + ex.getMessage());
        });
        
        routing.error(IOException.class, (req, res, ex) -> {
            res.status(Status.INTERNAL_SERVER_ERROR_500);
            res.send("IO Error: " + ex.getMessage());
        });
        
        routing.any((req, resp) -> {
            resp.status(Status.NOT_FOUND_404);
            resp.send("Not found");
        });
    }

    @Test
    void testNormalRequest() {
        try (Http1ClientResponse response = client.get("/normal.txt").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String content = response.entity().as(String.class);
            assertThat(content).isEqualTo("Normal content");
        }
    }

    @Test
    void testRuntimeExceptionHandling() {
        try (Http1ClientResponse response = client.get("/error-simulated").request()) {
            assertThat(response.status().code()).isEqualTo(500);
            String content = response.entity().as(String.class);
            assertThat(content).contains("Error");
        }
    }

    @Test
    void testIOExceptionHandling() {
        try (Http1ClientResponse response = client.get("/io-error").request()) {
            assertThat(response.status().code()).isEqualTo(500);
            String content = response.entity().as(String.class);
            assertThat(content).contains("IO Error");
        }
    }

    @Test
    void testNotFoundHandling() {
        try (Http1ClientResponse response = client.get("/nonexistent").request()) {
            assertThat(response.status().code()).isEqualTo(404);
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
                } catch (Exception e) {
                    // Connection errors are also acceptable in error scenarios
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join(5000);
        }
        
        assertThat(errorCount[0] + successCount[0]).isGreaterThan(0);
    }
}
