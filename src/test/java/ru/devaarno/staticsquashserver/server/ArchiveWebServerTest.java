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

import static org.assertj.core.api.Assertions.assertThat;

@ServerTest
class ArchiveWebServerTest {

    private final Http1Client client;

    ArchiveWebServerTest(Http1Client client) {
        this.client = client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder routing) {
        routing.get("/test.txt", (req, res) -> {
            res.send("Hello World");
        });
        
        routing.get("/nested/file.json", (req, res) -> {
            res.send("{\"key\":\"value\"}");
        });
        
        routing.any((req, resp) -> {
            resp.status(Status.NOT_FOUND_404);
            resp.send("Not found");
        });
    }

    @Test
    void testExistingFile() {
        try (Http1ClientResponse response = client.get("/test.txt").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).isEqualTo("Hello World");
        }
    }

    @Test
    void testNestedFile() {
        try (Http1ClientResponse response = client.get("/nested/file.json").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).isEqualTo("{\"key\":\"value\"}");
        }
    }

    @Test
    void testNotFound() {
        try (Http1ClientResponse response = client.get("/nonexistent.txt").request()) {
            assertThat(response.status().code()).isEqualTo(404);
            String entity = response.entity().as(String.class);
            assertThat(entity).contains("Not found");
        }
    }
}
