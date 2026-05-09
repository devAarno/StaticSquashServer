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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SimpleLogger utility class.
 */
class SimpleLoggerTest {

    @Test
    void testInfoLogging() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            SimpleLogger.info("Test info message");
            String output = outputStream.toString();
            assertThat(output).contains("[StaticSquashServer]");
            assertThat(output).contains("INFO");
            assertThat(output).contains("Test info message");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testWarnLogging() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(outputStream));
        
        try {
            SimpleLogger.warn("Test warning message");
            String output = outputStream.toString();
            assertThat(output).contains("[StaticSquashServer]");
            assertThat(output).contains("WARN");
            assertThat(output).contains("Test warning message");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void testErrorLogging() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(outputStream));
        
        try {
            SimpleLogger.error("Test error message");
            String output = outputStream.toString();
            assertThat(output).contains("[StaticSquashServer]");
            assertThat(output).contains("ERROR");
            assertThat(output).contains("Test error message");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void testErrorLoggingWithThrowable() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(outputStream));
        
        try {
            Exception testException = new RuntimeException("Test exception");
            SimpleLogger.error("Test error with exception", testException);
            String output = outputStream.toString();
            assertThat(output).contains("[StaticSquashServer]");
            assertThat(output).contains("ERROR");
            assertThat(output).contains("Test error with exception");
            assertThat(output).contains("java.lang.RuntimeException");
            assertThat(output).contains("Test exception");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void testWarnLoggingWithThrowable() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(outputStream));
        
        try {
            Exception testException = new IOException("Test IO exception");
            SimpleLogger.warn("Test warning with exception", testException);
            String output = outputStream.toString();
            assertThat(output).contains("[StaticSquashServer]");
            assertThat(output).contains("WARN");
            assertThat(output).contains("Test warning with exception");
            assertThat(output).contains("java.io.IOException");
        } finally {
            System.setErr(originalErr);
        }
    }
}
