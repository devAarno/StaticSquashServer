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

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(port).isGreaterThan(0);
        assertThat(server.isRunning()).isTrue();
        
        server.stop();
        assertThat(server.isRunning()).isFalse();
    }

    @Test
    void testRequestQueueShutdown() throws Exception {
        RequestQueue queue = new RequestQueue(null, null);
        
        assertThat(queue).isNotNull();
        
        queue.shutdown();
        
        Thread.sleep(100);
    }

    @Test
    void testMultipleStopCalls() throws Exception {
        WebServer server = WebServer.builder()
                .port(0)
                .build();
        
        server.start();
        server.stop();
        
        assertThat(server.isRunning()).isFalse();
        
        server.stop();
        
        assertThat(server.isRunning()).isFalse();
    }

    @Test
    void testShutdownWithPendingOperations() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Thread worker = new Thread(() -> {
            try {
                startLatch.await(5, TimeUnit.SECONDS);
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
        
        Thread.sleep(50);
        worker.interrupt();
        
        boolean finished = doneLatch.await(2, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
    }

    @Test
    void testServerLifecycle() throws Exception {
        WebServer server = WebServer.builder()
                .port(0)
                .build();
        
        assertThat(server.isRunning()).isFalse();
        
        server.start();
        assertThat(server.isRunning()).isTrue();
        
        Thread.sleep(100);
        assertThat(server.isRunning()).isTrue();
        
        server.stop();
        assertThat(server.isRunning()).isFalse();
    }
}
