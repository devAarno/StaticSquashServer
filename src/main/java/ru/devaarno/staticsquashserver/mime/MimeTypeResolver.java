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

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;

import java.util.Map;

/**
 * Resolves MIME types based on file extensions.
 * Optimized for Allure report files (HTML, JSON, JS, CSS).
 */
public final class MimeTypeResolver {
    
    private static final Map<String, MediaType> EXTENSION_TO_TYPE = Map.ofEntries(
        Map.entry("html", MediaTypes.TEXT_HTML),
        Map.entry("htm", MediaTypes.TEXT_HTML),
        Map.entry("css", MediaTypes.create("text/css")),
        Map.entry("js", MediaTypes.create("text/javascript")),
        Map.entry("json", MediaTypes.APPLICATION_JSON),
        Map.entry("png", MediaTypes.create("image/png")),
        Map.entry("jpg", MediaTypes.create("image/jpeg")),
        Map.entry("jpeg", MediaTypes.create("image/jpeg")),
        Map.entry("gif", MediaTypes.create("image/gif")),
        Map.entry("svg", MediaTypes.create("image/svg+xml")),
        Map.entry("txt", MediaTypes.TEXT_PLAIN),
        Map.entry("xml", MediaTypes.APPLICATION_XML),
        Map.entry("ico", MediaTypes.create("image/x-icon")),
        Map.entry("woff", MediaTypes.create("font/woff")),
        Map.entry("woff2", MediaTypes.create("font/woff2")),
        Map.entry("ttf", MediaTypes.create("font/ttf")),
        Map.entry("eot", MediaTypes.create("application/vnd.ms-fontobject"))
    );
    
    private MimeTypeResolver() {
    }
    
    public static MediaType resolve(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return MediaTypes.APPLICATION_OCTET_STREAM;
        }
        
        String ext = fileName.substring(lastDot + 1).toLowerCase();
        return EXTENSION_TO_TYPE.getOrDefault(ext, MediaTypes.APPLICATION_OCTET_STREAM);
    }
}
