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

import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveWebServerIntegrationTest {

    private static ArchiveWebServer server;
    private static Http1Client client;
    private static int serverPort;
    private static final Path TEST_ARCHIVE = Paths.get("src/test/resources/test_archives/test.zip");

    @BeforeAll
    static void setUp() {
        var config = CliConfigForTest.create(TEST_ARCHIVE);
        server = new ArchiveWebServer(config);
        server.start();
        serverPort = server.port();
        
        client = Http1Client.builder()
                .baseUri("http://localhost:" + serverPort)
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void testHtmlFile() {
        try (Http1ClientResponse response = client.get("/index.html").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).contains("<html>");
        }
    }

    @Test
    void testNestedJsonFile() {
        try (Http1ClientResponse response = client.get("/data/report.json").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).contains("\"tests\"");
            assertThat(entity).contains("\"passed\"");
        }
    }

    @Test
    void testCssFile() {
        try (Http1ClientResponse response = client.get("/css/style.css").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).contains("margin");
        }
    }

    @Test
    void testNotFound() {
        try (Http1ClientResponse response = client.get("/nonexistent.txt").request()) {
            assertThat(response.status().code()).isEqualTo(404);
        }
    }

    @Test
    void testDeeplyNestedFile() {
        try (Http1ClientResponse response = client.get("/data/nested/deep/level3.json").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).contains("\"level\"");
        }
    }

    @Test
    void testFileWithSpaces() {
        try (Http1ClientResponse response = client.get("/file with spaces.txt").request()) {
            assertThat(response.status().code()).isEqualTo(200);
        }
    }

    @Test
    void testHiddenFile() {
        try (Http1ClientResponse response = client.get("/.hidden_file").request()) {
            assertThat(response.status().code()).isEqualTo(200);
        }
    }

    @Test
    void testAllureReportIndex() {
        try (Http1ClientResponse response = client.get("/allure-report/index.html").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).contains("<html>");
        }
    }

    @Test
    void testAllureWidgetJson() {
        try (Http1ClientResponse response = client.get("/allure-report/widgets/summary.json").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).contains("{");
        }
    }

    @Test
    void testImageFile() {
        try (Http1ClientResponse response = client.get("/images/logo.png").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            assertThat(response.headers().first(io.helidon.http.HeaderNames.CONTENT_TYPE).orElse("")).isEqualTo("image/png");
        }
    }

    @Test
    void testSvgFile() {
        try (Http1ClientResponse response = client.get("/images/icons/small.svg").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).contains("<svg");
        }
    }

    @Test
    void testEmptyFile() {
        try (Http1ClientResponse response = client.get("/empty_file.txt").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            assertThat(response.headers().first(io.helidon.http.HeaderNames.CONTENT_LENGTH).orElse("0")).isEqualTo("0");
        }
    }

    @Test
    void testJavaScriptFile() {
        try (Http1ClientResponse response = client.get("/js/app.js").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).contains("console");
        }
    }

    @Test
    void testNestedJavaScriptFile() {
        try (Http1ClientResponse response = client.get("/js/lib/vendor.js").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).contains("Vendor");
        }
    }
}
