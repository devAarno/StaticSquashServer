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
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.devaarno.staticsquashserver.server.shared.CliConfigBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static io.helidon.http.HeaderNames.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@TestInstance(PER_CLASS)
class TarXZBigArchiveWebServerIntegrationTest {

    private static final int READ_PACK = 81920;

    @AutoClose("stop")
    private static ArchiveWebServer server;
    private static Http1Client client;

    private static final Path BIG_ARCHIVE = assertDoesNotThrow(
            () -> Path.of(Objects.requireNonNull(
                    TarXZBigArchiveWebServerIntegrationTest.class.getResource("/test_archives/big.tar.xz")
            ).toURI()),
            "Test archive URI should be resolved"
    );

    private static final long AA_TXT_CRC32 = 1292241898L;
    private static final long A_TXT_CRC32 = 315542328L;
    private static final long B_TXT_CRC32 = 926581413L;

    private static final String MSG_RESPONSE_STATUS = "Response status should be";
    private static final String MSG_CONTENT_TYPE = "Content type should be";
    private static final String MSG_ENTITY_EXISTS = "Response should have entity";
    private static final String MSG_CRC32_MISMATCH = "CRC32 mismatch for %s";

    @BeforeAll
    static void setUp() {
        server = new ArchiveWebServer(
                CliConfigBuilder.build(BIG_ARCHIVE)
        );
        server.start();
        
        client = Http1Client.builder()
                .baseUri("http://localhost:" + server.port())
                .build();
    }

    @ParameterizedTest(name = "Test big file: {0}")
    @MethodSource("bigFilesWithCrc32")
    void testBigFileWithCrc32(final String path, final String contentType, final long expectedCrc32) throws IOException {
        try (final Http1ClientResponse response = client.get(path).request()) {
            assertEquals(200, response.status().code(), MSG_RESPONSE_STATUS + " 200 for " + path);
            assertEquals(contentType, response.headers().first(CONTENT_TYPE).orElse(""), MSG_CONTENT_TYPE + " " + path);
            assertTrue(response.entity().hasEntity(), MSG_ENTITY_EXISTS + " " + path);
            final CRC32 crc32 = new CRC32();
            byte[] buffer = new byte[READ_PACK];
            int readLength;
            try (final var data = response.entity().inputStream()) {
                while (true) {
                    readLength = data.read(buffer);
                    if (0 > readLength) {
                        break;
                    }
                    crc32.update(buffer, 0, readLength);
                }
            }
            assertEquals(expectedCrc32, crc32.getValue(), String.format(MSG_CRC32_MISMATCH, path));
        }
    }

    static Stream<Arguments> bigFilesWithCrc32() {
        return Stream.of(
                arguments("/aa.txt", MediaTypes.TEXT_PLAIN.text(), AA_TXT_CRC32),
                arguments("/a.txt", MediaTypes.TEXT_PLAIN.text(), A_TXT_CRC32),
                arguments("/b.txt", MediaTypes.TEXT_PLAIN.text(), B_TXT_CRC32)
        );
    }
}
