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

import java.nio.file.Path;

public final class CliConfig {

    @Parameter(names = {"-h", "--help"}, help = true, description = "Show a help")
    private boolean help;

    @Parameter(names = {"-p", "--port"}, description = "Server port (default: 8080)", order = 1, validateValueWith = PortValidator.class)
    private int port = 8080;

    @Parameter(names = {"-a", "--archive"}, description = "Path to archive file", order = 2, validateValueWith = ArchiveValidator.class, required = true)
    private Path archivePath;

    public CliConfig() {
    }

    public int getPort() {
        return port;
    }

    public Path getArchivePath() {
        return archivePath;
    }

    public boolean isHelp() {
        return help;
    }

    public static CliConfig parse(final String[] args) {
        final var config = new CliConfig();
        final var jc = JCommander
                .newBuilder()
                .addObject(config)
                .build();
        jc.parse(args);

        if (config.isHelp()) {
            jc.usage();
        }
        return config;
    }
}
