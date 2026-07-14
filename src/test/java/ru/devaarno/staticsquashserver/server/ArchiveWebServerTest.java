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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ServerTest
class ArchiveWebServerTest {

    private final Http1Client client;

    ArchiveWebServerTest(Http1Client client) {
        this.client = client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder routing) {
        routing.get("/test.txt", (req, res) -> res.send("Hello World"));
        
        routing.get("/nested/file.json", (req, res) -> res.send("{\"key\":\"value\"}"));
        
        routing.any((req, resp) -> {
            resp.status(Status.NOT_FOUND_404);
            resp.send("Not found");
        });
    }

    @Test
    void testExistingFile() {
        try (Http1ClientResponse response = client.get("/test.txt").request()) {
            assertEquals(200, response.status().code(), "Response status should be 200 for /test.txt");
            String entity = response.entity().as(String.class);
            assertEquals("Hello World", entity, "Content should match for /test.txt");
        }
    }

    @Test
    void testNestedFile() {
        try (Http1ClientResponse response = client.get("/nested/file.json").request()) {
            assertEquals(200, response.status().code(), "Response status should be 200 for /nested/file.json");
            String entity = response.entity().as(String.class);
            assertEquals("{\"key\":\"value\"}", entity, "Content should match for /nested/file.json");
        }
    }

    @Test
    void testNotFound() {
        try (Http1ClientResponse response = client.get("/nonexistent.txt").request()) {
            assertEquals(404, response.status().code(), "Response status should be 404 for /nonexistent.txt");
            String entity = response.entity().as(String.class);
            assertTrue(entity.contains("Not found"), "Content should contain 'Not found' for /nonexistent.txt");
        }
    }
}
