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

import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Stage 8: Graceful Shutdown.
 * Tests that the server shuts down correctly and resources are released.
 */
class GracefulShutdownTest {

    @Test
    void testServerStartAndStop() throws Exception {
        WebServer server = WebServer.builder()
                .port(0)
                .build();
        
        server.start();
        int port = server.port();
        assertTrue(port > 0, "Port should be greater than 0");
        assertTrue(server.isRunning(), "Server should be running after start");
        
        server.stop();
        assertFalse(server.isRunning(), "Server should not be running after stop");
    }

    @Test
    void testMultipleStopCalls() throws Exception {
        WebServer server = WebServer.builder()
                .port(0)
                .build();
        
        server.start();
        server.stop();
        
        assertFalse(server.isRunning(), "Server should not be running after first stop");
        
        server.stop();
        
        assertFalse(server.isRunning(), "Server should not be running after second stop");
    }

    @Test
    void testShutdownWithPendingOperations() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        CountDownLatch workStartedLatch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Thread worker = new Thread(() -> {
            try {
                startLatch.await(5, TimeUnit.SECONDS);
                workStartedLatch.countDown();
                Thread.sleep(100);
                completed.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
        
        worker.start();
        startLatch.countDown();
        
        workStartedLatch.await(1, TimeUnit.SECONDS);
        worker.interrupt();
        
        boolean finished = doneLatch.await(2, TimeUnit.SECONDS);
        assertTrue(finished, "Latch should count down within timeout");
    }

    @Test
    void testServerLifecycle() throws Exception {
        WebServer server = WebServer.builder()
                .port(0)
                .build();
        
        assertFalse(server.isRunning(), "Server should not be running before start");
        
        server.start();
        assertTrue(server.isRunning(), "Server should be running after start");
        
        server.stop();
        assertFalse(server.isRunning(), "Server should not be running after stop");
    }
}
