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

import ru.devaarno.staticsquashserver.cli.CliConfig;
import ru.devaarno.staticsquashserver.logging.SimpleLogger;
import ru.devaarno.staticsquashserver.server.ArchiveWebServer;

public final class Main {
    static void main(final String[] args) {
        try {
            
            ArchiveWebServer server = new ArchiveWebServer(
                    CliConfig.parse(args)
            );
            server.start();
            
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            
            server.awaitShutdown();
            
        } catch (final IllegalArgumentException e) {
            SimpleLogger.error("Configuration error: " + e.getMessage());
            System.exit(1);
        } catch (final Exception e) {
            SimpleLogger.error("Application error: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}
