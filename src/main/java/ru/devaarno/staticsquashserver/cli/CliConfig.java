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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.nio.file.Files;
import java.nio.file.Path;

public final class CliConfig {
    @Parameter(names = {"-p", "--port"}, description = "Server port (default: 8080)", order = 1)
    private int port = 8080;

    @Parameter(names = {"-a", "--archive"}, description = "Path to archive file", order = 2)
    private String archivePath;

    public CliConfig() {
    }

    public int getPort() {
        return port;
    }

    public String getArchivePath() {
        return archivePath;
    }

    public void validate() {
        if (archivePath == null || archivePath.isEmpty()) {
            throw new ParameterException("Archive path is required. Use --archive or -a");
        }

        Path path = Path.of(archivePath);
        if (!Files.exists(path)) {
            throw new ParameterException("Archive file does not exist: " + archivePath);
        }

        if (!Files.isReadable(path)) {
            throw new ParameterException("Archive file is not readable: " + archivePath);
        }

        if (port < 1 || port > 65535) {
            throw new ParameterException("Port must be between 1 and 65535: " + port);
        }
    }

    public static CliConfig parse(String[] args) {
        CliConfig config = new CliConfig();
        JCommander jc = JCommander.newBuilder()
                .addObject(config)
                .build();
        jc.parse(args);
        config.validate();
        return config;
    }
}
