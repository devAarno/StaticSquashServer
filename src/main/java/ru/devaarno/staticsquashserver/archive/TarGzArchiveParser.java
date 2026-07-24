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

package ru.devaarno.staticsquashserver.archive;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Parser for TAR.GZ archive format.
 */
public final class TarGzArchiveParser extends AbstractTarArchiveParser implements ArchiveParser {

    public TarGzArchiveParser(final Path actualArchivePath) {
        super(actualArchivePath);
    }

    @Override
    public ArchiveFormat format() {
        return ArchiveFormat.TAR_GZ;
    }

    @Override
    public CompressorInputStream getCompressionChainMethod(final InputStream inputStream) throws IOException {
        return new GzipCompressorInputStream(inputStream);
    }
}
