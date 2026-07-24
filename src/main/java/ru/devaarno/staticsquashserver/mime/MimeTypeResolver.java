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

/**
 * Resolves MIME types based on file extensions.
 * Optimized for Allure report files (HTML, JSON, JS, CSS).
 */
public final class MimeTypeResolver {
    
    private MimeTypeResolver() {
    }
    
    public static MediaType resolve(final String fileName) {
        final var lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return MediaTypes.APPLICATION_OCTET_STREAM;
        }
        
        final var ext = fileName.substring(lastDot + 1).toLowerCase();
        return switch (ext) {
            case "html", "htm" -> MediaTypes.TEXT_HTML;
            case "css" -> MediaTypes.create("text/css");
            case "js" -> MediaTypes.create("text/javascript");
            case "json" -> MediaTypes.APPLICATION_JSON;
            case "png" -> MediaTypes.create("image/png");
            case "jpg", "jpeg" -> MediaTypes.create("image/jpeg");
            case "gif" -> MediaTypes.create("image/gif");
            case "svg" -> MediaTypes.create("image/svg+xml");
            case "txt" -> MediaTypes.TEXT_PLAIN;
            case "xml" -> MediaTypes.APPLICATION_XML;
            case "ico" -> MediaTypes.create("image/x-icon");
            case "woff" -> MediaTypes.create("font/woff");
            case "woff2" -> MediaTypes.create("font/woff2");
            case "ttf" -> MediaTypes.create("font/ttf");
            case "eot" -> MediaTypes.create("application/vnd.ms-fontobject");
            default -> MediaTypes.APPLICATION_OCTET_STREAM;
        };
    }
}
