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

import io.helidon.common.media.type.MediaTypes;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.CRC32;

import static io.helidon.http.HeaderNames.CONTENT_LENGTH;
import static io.helidon.http.HeaderNames.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.*;

class ArchiveWebServerIntegrationTest {

    private static ArchiveWebServer server;
    private static Http1Client client;
    private static int serverPort;

    private static final Path TEST_ARCHIVE = assertDoesNotThrow(
            () -> Path.of(Objects.requireNonNull(
                    ArchiveWebServerIntegrationTest.class.getResource("/test_archives/test.zip")
            ).toURI()),
            "Test TAR.XZ archive URI should be resolved without exceptions"
    );

    private static final long INDEX_HTML_CRC32 = 3005742938L;
    private static final long REPORT_JSON_CRC32 = 2840432823L;
    private static final long STYLE_CSS_CRC32 = 791642431L;
    private static final long LEVEL3_JSON_CRC32 = 4118783144L;
    private static final long FILE_WITH_SPACES_CRC32 = 832580676L;
    private static final long HIDDEN_FILE_CRC32 = 3550211444L;
    private static final long ALLURE_INDEX_HTML_CRC32 = 925851897L;
    private static final long ALLURE_SUMMARY_JSON_CRC32 = 1374149304L;
    private static final long LOGO_PNG_CRC32 = 3173656689L;
    private static final long SMALL_SVG_CRC32 = 2192869760L;
    private static final long APP_JS_CRC32 = 1244231727L;
    private static final long VENDOR_JS_CRC32 = 3801644746L;
    private static final String CRC32_MISMATCH = "CRC32 checksum mismatch";

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
            assertEquals(200, response.status().code());
            assertEquals(MediaTypes.TEXT_HTML.text(), response.headers().first(CONTENT_TYPE).orElse(""));
            assertTrue(response.entity().hasEntity());
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(INDEX_HTML_CRC32, crc32.getValue(), CRC32_MISMATCH);
        }
    }

    @Test
    void testNestedJsonFile() {
        try (Http1ClientResponse response = client.get("/data/report.json").request()) {
            assertEquals(200, response.status().code());
            assertEquals(MediaTypes.APPLICATION_JSON.text(), response.headers().first(CONTENT_TYPE).orElse(""));
            assertTrue(response.entity().hasEntity());
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(REPORT_JSON_CRC32, crc32.getValue(), CRC32_MISMATCH);
        }
    }

    @Test
    void testCssFile() {
        try (Http1ClientResponse response = client.get("/css/style.css").request()) {
            assertEquals(200, response.status().code());
            assertEquals("text/css", response.headers().first(CONTENT_TYPE).orElse(""));
            assertTrue(response.entity().hasEntity());
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(STYLE_CSS_CRC32, crc32.getValue(), CRC32_MISMATCH);
        }
    }

    @Test
    void testNotFound() {
        try (Http1ClientResponse response = client.get("/nonexistent.txt").request()) {
            assertEquals(404, response.status().code());
            assertTrue(response.entity().hasEntity());
            assertEquals("Not found", response.entity().as(String.class));
        }
    }

    @Test
    void testDeeplyNestedFile() {
        try (Http1ClientResponse response = client.get("/data/nested/deep/level3.json").request()) {
            assertEquals(200, response.status().code());
            assertEquals(MediaTypes.APPLICATION_JSON.text(), response.headers().first(CONTENT_TYPE).orElse(""));
            assertTrue(response.entity().hasEntity());
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(LEVEL3_JSON_CRC32, crc32.getValue(), CRC32_MISMATCH);
        }
    }

    @Test
    void testFileWithSpaces() {
        try (Http1ClientResponse response = client.get("/file with spaces.txt").request()) {
            assertEquals(200, response.status().code());
            assertEquals(MediaTypes.TEXT_PLAIN.text(), response.headers().first(CONTENT_TYPE).orElse(""));
            assertTrue(response.entity().hasEntity());
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(FILE_WITH_SPACES_CRC32, crc32.getValue(), CRC32_MISMATCH);
        }
    }

    @Test
    void testHiddenFile() {
        try (Http1ClientResponse response = client.get("/.hidden_file").request()) {
            assertEquals(200, response.status().code());
            assertEquals(MediaTypes.APPLICATION_OCTET_STREAM.text(), response.headers().first(CONTENT_TYPE).orElse(""));
            assertTrue(response.entity().hasEntity());
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(HIDDEN_FILE_CRC32, crc32.getValue(), CRC32_MISMATCH);
        }
    }

    @Test
    void testAllureReportIndex() {
        try (Http1ClientResponse response = client.get("/allure-report/index.html").request()) {
            assertEquals(200, response.status().code());
            assertEquals(MediaTypes.TEXT_HTML.text(), response.headers().first(CONTENT_TYPE).orElse(""));
            assertTrue(response.entity().hasEntity());
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(ALLURE_INDEX_HTML_CRC32, crc32.getValue(), CRC32_MISMATCH);
        }
    }

    @Test
    void testAllureWidgetJson() {
        try (Http1ClientResponse response = client.get("/allure-report/widgets/summary.json").request()) {
            assertEquals(200, response.status().code());
            assertEquals(MediaTypes.APPLICATION_JSON.text(), response.headers().first(CONTENT_TYPE).orElse(""));
            assertTrue(response.entity().hasEntity());
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(ALLURE_SUMMARY_JSON_CRC32, crc32.getValue(), CRC32_MISMATCH);
        }
    }

    @Test
    void testImageFile() {
        try (Http1ClientResponse response = client.get("/images/logo.png").request()) {
            assertEquals(200, response.status().code());
            assertEquals("image/png", response.headers().first(CONTENT_TYPE).orElse(""));
            assertTrue(response.entity().hasEntity());
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(LOGO_PNG_CRC32, crc32.getValue(), CRC32_MISMATCH);
        }
    }

    @Test
    void testSvgFile() {
        try (Http1ClientResponse response = client.get("/images/icons/small.svg").request()) {
            assertEquals(200, response.status().code());
            assertEquals("image/svg+xml", response.headers().first(CONTENT_TYPE).orElse(""));
            assertTrue(response.entity().hasEntity());
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(SMALL_SVG_CRC32, crc32.getValue(), CRC32_MISMATCH);
        }
    }

    @Test
    void testEmptyFile() {
        try (Http1ClientResponse response = client.get("/empty_file.txt").request()) {
            assertEquals(200, response.status().code());
            assertEquals(MediaTypes.TEXT_PLAIN.text(), response.headers().first(CONTENT_TYPE).orElse(""));
            assertEquals("0", response.headers().first(CONTENT_LENGTH).orElse("0"));
            assertFalse(response.entity().hasEntity());
        }
    }

    @Test
    void testJavaScriptFile() {
        try (Http1ClientResponse response = client.get("/js/app.js").request()) {
            assertEquals(200, response.status().code());
            assertEquals("text/javascript", response.headers().first(CONTENT_TYPE).orElse(""));
            assertTrue(response.entity().hasEntity());
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(APP_JS_CRC32, crc32.getValue(), CRC32_MISMATCH);
        }
    }

    @Test
    void testNestedJavaScriptFile() {
        try (Http1ClientResponse response = client.get("/js/lib/vendor.js").request()) {
            assertEquals(200, response.status().code());
            assertEquals("text/javascript", response.headers().first(CONTENT_TYPE).orElse(""));
            assertTrue(response.entity().hasEntity());
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(VENDOR_JS_CRC32, crc32.getValue(), CRC32_MISMATCH);
        }
    }
}
