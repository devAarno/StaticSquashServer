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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static io.helidon.http.HeaderNames.CONTENT_LENGTH;
import static io.helidon.http.HeaderNames.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ZipArchiveWebServerIntegrationTest {

    private static ArchiveWebServer server;
    private static Http1Client client;
    private static int serverPort;

    private static final Path TEST_ARCHIVE = assertDoesNotThrow(
            () -> Path.of(Objects.requireNonNull(
                    ZipArchiveWebServerIntegrationTest.class.getResource("/test_archives/test.zip")
            ).toURI()),
            "Test archive URI should be resolved"
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

    private static final String MSG_RESPONSE_STATUS = "Response status should be";
    private static final String MSG_CONTENT_TYPE = "Content type should be";
    private static final String MSG_ENTITY_EXISTS = "Response should have entity";
    private static final String MSG_CRC32_MISMATCH = "CRC32 mismatch for %s";

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

    @ParameterizedTest(name = "Test file: {0}")
    @MethodSource("filesWithCrc32")
    void testFileWithCrc32(String path, String contentType, long expectedCrc32) {
        try (Http1ClientResponse response = client.get(path).request()) {
            assertEquals(200, response.status().code(), MSG_RESPONSE_STATUS + " 200 for " + path);
            assertEquals(contentType, response.headers().first(CONTENT_TYPE).orElse(""), MSG_CONTENT_TYPE + " " + path);
            assertTrue(response.entity().hasEntity(), MSG_ENTITY_EXISTS + " " + path);
            final CRC32 crc32 = new CRC32();
            crc32.update(response.entity().as(byte[].class));
            assertEquals(expectedCrc32, crc32.getValue(), String.format(MSG_CRC32_MISMATCH, path));
        }
    }

    @Test
    void testNotFound() {
        try (Http1ClientResponse response = client.get("/nonexistent.txt").request()) {
            assertEquals(404, response.status().code(), "404 status for nonexistent file");
            assertTrue(response.entity().hasEntity(), "Response should have entity for 404");
            assertEquals("Not found", response.entity().as(String.class), "404 response body");
        }
    }

    @Test
    void testEmptyFile() {
        try (Http1ClientResponse response = client.get("/empty_file.txt").request()) {
            assertEquals(200, response.status().code(), MSG_RESPONSE_STATUS + " 200 for empty file");
            assertEquals(MediaTypes.TEXT_PLAIN.text(), response.headers().first(CONTENT_TYPE).orElse(""), MSG_CONTENT_TYPE + " empty file");
            assertEquals("0", response.headers().first(CONTENT_LENGTH).orElse("0"), "Content length should be 0");
            assertFalse(response.entity().hasEntity(), "Empty file should not have entity");
        }
    }

    static Stream<Arguments> filesWithCrc32() {
        return Stream.of(
                arguments("/index.html", MediaTypes.TEXT_HTML.text(), INDEX_HTML_CRC32),
                arguments("/data/report.json", MediaTypes.APPLICATION_JSON.text(), REPORT_JSON_CRC32),
                arguments("/css/style.css", "text/css", STYLE_CSS_CRC32),
                arguments("/data/nested/deep/level3.json", MediaTypes.APPLICATION_JSON.text(), LEVEL3_JSON_CRC32),
                arguments("/file with spaces.txt", MediaTypes.TEXT_PLAIN.text(), FILE_WITH_SPACES_CRC32),
                arguments("/.hidden_file", MediaTypes.APPLICATION_OCTET_STREAM.text(), HIDDEN_FILE_CRC32),
                arguments("/allure-report/index.html", MediaTypes.TEXT_HTML.text(), ALLURE_INDEX_HTML_CRC32),
                arguments("/allure-report/widgets/summary.json", MediaTypes.APPLICATION_JSON.text(), ALLURE_SUMMARY_JSON_CRC32),
                arguments("/images/logo.png", "image/png", LOGO_PNG_CRC32),
                arguments("/images/icons/small.svg", "image/svg+xml", SMALL_SVG_CRC32),
                arguments("/js/app.js", "text/javascript", APP_JS_CRC32),
                arguments("/js/lib/vendor.js", "text/javascript", VENDOR_JS_CRC32)
        );
    }
}
