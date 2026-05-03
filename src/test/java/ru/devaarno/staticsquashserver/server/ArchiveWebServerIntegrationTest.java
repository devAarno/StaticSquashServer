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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveWebServerIntegrationTest {

    private static ArchiveWebServer server;
    private static Http1Client client;
    private static Path testArchive;
    private static int serverPort;

    @BeforeAll
    static void setUp() throws IOException {
        Path tempDir = Files.createTempDirectory("test-archive");
        testArchive = createTestArchive(tempDir);
        
        var config = CliConfigForTest.create(testArchive);
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
        if (testArchive != null) {
            try {
                Files.deleteIfExists(testArchive);
                Files.deleteIfExists(testArchive.getParent());
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private static Path createTestArchive(Path tempDir) throws IOException {
        Path archivePath = tempDir.resolve("test.zip");
        
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(archivePath))) {
            ZipEntry entry1 = new ZipEntry("index.html");
            zos.putNextEntry(entry1);
            zos.write("<html><body>Hello</body></html>".getBytes());
            zos.closeEntry();
            
            ZipEntry entry2 = new ZipEntry("data/test.json");
            zos.putNextEntry(entry2);
            zos.write("{\"test\":true}".getBytes());
            zos.closeEntry();
            
            ZipEntry entry3 = new ZipEntry("style.css");
            zos.putNextEntry(entry3);
            zos.write("body { color: black; }".getBytes());
            zos.closeEntry();
        }
        
        return archivePath;
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
        try (Http1ClientResponse response = client.get("/data/test.json").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).isEqualTo("{\"test\":true}");
        }
    }

    @Test
    void testCssFile() {
        try (Http1ClientResponse response = client.get("/style.css").request()) {
            assertThat(response.status().code()).isEqualTo(200);
            String entity = response.entity().as(String.class);
            assertThat(entity).contains("body");
        }
    }

    @Test
    void testNotFound() {
        try (Http1ClientResponse response = client.get("/nonexistent.txt").request()) {
            assertThat(response.status().code()).isEqualTo(404);
        }
    }
}
