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

package ru.devaarno.staticsquashserver.mime;

import io.helidon.common.media.type.MediaTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MIME type resolution.
 */
class MimeTypeResolverTest {
    
    @Test
    void testHtmlTypes() {
        assertEquals(MediaTypes.TEXT_HTML, MimeTypeResolver.resolve("index.html"));
        assertEquals(MediaTypes.TEXT_HTML, MimeTypeResolver.resolve("report.htm"));
        assertEquals(MediaTypes.TEXT_HTML, MimeTypeResolver.resolve("UPPERCASE.HTML"));
    }
    
    @Test
    void testCssType() {
        assertEquals(MediaTypes.create("text/css"), MimeTypeResolver.resolve("style.css"));
    }
    
    @Test
    void testJavaScriptTypes() {
        assertEquals(MediaTypes.APPLICATION_JAVASCRIPT, MimeTypeResolver.resolve("app.js"));
        assertEquals(MediaTypes.APPLICATION_JAVASCRIPT, MimeTypeResolver.resolve("vendor.js"));
    }
    
    @Test
    void testJsonType() {
        assertEquals(MediaTypes.APPLICATION_JSON, MimeTypeResolver.resolve("data.json"));
        assertEquals(MediaTypes.APPLICATION_JSON, MimeTypeResolver.resolve("report.json"));
    }
    
    @Test
    void testImageTypes() {
        assertEquals(MediaTypes.create("image/png"), MimeTypeResolver.resolve("logo.png"));
        assertEquals(MediaTypes.create("image/jpeg"), MimeTypeResolver.resolve("photo.jpg"));
        assertEquals(MediaTypes.create("image/jpeg"), MimeTypeResolver.resolve("image.jpeg"));
        assertEquals(MediaTypes.create("image/gif"), MimeTypeResolver.resolve("animation.gif"));
        assertEquals(MediaTypes.create("image/svg+xml"), MimeTypeResolver.resolve("icon.svg"));
    }
    
    @Test
    void testTextType() {
        assertEquals(MediaTypes.TEXT_PLAIN, MimeTypeResolver.resolve("readme.txt"));
        assertEquals(MediaTypes.TEXT_PLAIN, MimeTypeResolver.resolve("data.txt"));
    }
    
    @Test
    void testXmlType() {
        assertEquals(MediaTypes.APPLICATION_XML, MimeTypeResolver.resolve("config.xml"));
    }
    
    @Test
    void testFavicon() {
        var favicon = MimeTypeResolver.resolve("favicon.ico");
        assertEquals("image/x-icon", favicon.text());
    }
    
    @Test
    void testUnknownExtension() {
        var type = MimeTypeResolver.resolve("file.unknown");
        assertEquals(MediaTypes.APPLICATION_OCTET_STREAM, type);
    }
    
    @Test
    void testNoExtension() {
        var type = MimeTypeResolver.resolve(".hidden_file");
        assertEquals(MediaTypes.APPLICATION_OCTET_STREAM, type);
    }
    
    @Test
    void testCaseInsensitiveExtension() {
        assertEquals(MediaTypes.TEXT_HTML, MimeTypeResolver.resolve("file.HTML"));
        assertEquals(MediaTypes.TEXT_HTML, MimeTypeResolver.resolve("file.Html"));
        assertEquals(MediaTypes.APPLICATION_JSON, MimeTypeResolver.resolve("data.JSON"));
    }
    
    @Test
    void testExtensionWithMultipleDots() {
        assertEquals(MediaTypes.TEXT_HTML, MimeTypeResolver.resolve("file.test.html"));
        assertEquals(MediaTypes.APPLICATION_JSON, MimeTypeResolver.resolve("data.v1.json"));
    }
}
