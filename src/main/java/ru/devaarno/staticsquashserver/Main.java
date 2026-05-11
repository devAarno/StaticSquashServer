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

package ru.devaarno.staticsquashserver;

import io.helidon.logging.common.LogConfig;
import ru.devaarno.staticsquashserver.cli.CliConfig;
import ru.devaarno.staticsquashserver.server.ArchiveWebServer;
import ru.devaarno.staticsquashserver.server.RequestQueue;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    static void main(final String[] args) {

        // load logging configuration
        LogConfig.configureRuntime();

        try {
            
            ArchiveWebServer server = new ArchiveWebServer(
                    CliConfig.parse(args)
            );
            server.start();
            
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            
            server.awaitShutdown();
            
        } catch (final IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Configuration error: {0}", e.getMessage());
            System.exit(1);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Application error: {0}", e.getMessage());
            System.exit(1);
        }
    }
}
