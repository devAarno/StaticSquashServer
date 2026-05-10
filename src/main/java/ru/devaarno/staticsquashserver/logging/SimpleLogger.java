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

package ru.devaarno.staticsquashserver.logging;

/**
 * Simple logger for minimal logging without external dependencies.
 * Uses System.out and System.err for output.
 */
public final class SimpleLogger {
    private static final String PREFIX = "[StaticSquashServer]";
    
    private SimpleLogger() {
        // Utility class
    }
    
    public static void info(final String message) {
        System.out.println(PREFIX + " INFO: " + message);
    }
    
    public static void warn(final String message) {
        System.err.println(PREFIX + " WARN: " + message);
    }
    
    public static void warn(final String message, final Throwable t) {
        System.err.println(PREFIX + " WARN: " + message);
        t.printStackTrace(System.err);
    }
    
    public static void error(final String message) {
        System.err.println(PREFIX + " ERROR: " + message);
    }
    
    public static void error(final String message, final Throwable t) {
        System.err.println(PREFIX + " ERROR: " + message);
        t.printStackTrace(System.err);
    }
}
