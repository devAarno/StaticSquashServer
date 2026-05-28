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

package ru.devaarno.staticsquashserver.cli;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ArchiveValidator implements IValueValidator<Path> {

    private static final Logger LOGGER = Logger.getLogger(ArchiveValidator.class.getName());

    @Override
    public void validate(final String name, final Path value) throws ParameterException {
        final var file = value.toFile();

        if (!(file.exists())) {
            LOGGER.log(Level.SEVERE, "File is not existed: {0}", value);
            throw new ParameterException("File is not existed");
        }

        if (!(file.isFile())) {
            LOGGER.log(Level.SEVERE, "File is not a regular file: {0}", value);
            throw new ParameterException("File is not a regular file");
        }

        if (!(file.canRead())) {
            LOGGER.log(Level.SEVERE, "File is not readable: {0}", value);
            throw new ParameterException("File is not readable");
        }
    }
}
