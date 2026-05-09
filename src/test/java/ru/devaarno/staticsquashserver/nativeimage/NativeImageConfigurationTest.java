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

package ru.devaarno.staticsquashserver.nativeimage;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GraalVM Native Image configuration files.
 * Verifies that all required configuration files exist and are valid.
 */
class NativeImageConfigurationTest {

    private static final String NATIVE_IMAGE_DIR = "src/main/resources/META-INF/native-image/ru.devaarno.staticsquashserver/static-squash-server";

    @Test
    void testReflectConfigExists() throws IOException {
        Path reflectConfig = Paths.get(NATIVE_IMAGE_DIR, "reflect-config.json");
        assertThat(reflectConfig).exists();
        assertThat(reflectConfig).isReadable();
    }

    @Test
    void testResourceConfigExists() throws IOException {
        Path resourceConfig = Paths.get(NATIVE_IMAGE_DIR, "resource-config.json");
        assertThat(resourceConfig).exists();
        assertThat(resourceConfig).isReadable();
    }

    @Test
    void testProxyConfigExists() throws IOException {
        Path proxyConfig = Paths.get(NATIVE_IMAGE_DIR, "proxy-config.json");
        assertThat(proxyConfig).exists();
        assertThat(proxyConfig).isReadable();
    }

    @Test
    void testReflectConfigContainsCliConfig() throws IOException {
        Path reflectConfig = Paths.get(NATIVE_IMAGE_DIR, "reflect-config.json");
        String content = Files.readString(reflectConfig);
        
        assertThat(content).contains("ru.devaarno.staticsquashserver.cli.CliConfig");
        assertThat(content).contains("allDeclaredFields");
        assertThat(content).contains("allDeclaredConstructors");
        assertThat(content).contains("allDeclaredMethods");
    }

    @Test
    void testReflectConfigContainsJCommanderParameter() throws IOException {
        Path reflectConfig = Paths.get(NATIVE_IMAGE_DIR, "reflect-config.json");
        String content = Files.readString(reflectConfig);
        
        assertThat(content).contains("com.beust.jcommander.Parameter");
    }

    @Test
    void testResourceConfigContainsPatterns() throws IOException {
        Path resourceConfig = Paths.get(NATIVE_IMAGE_DIR, "resource-config.json");
        String content = Files.readString(resourceConfig);
        
        assertThat(content).contains("pattern");
        assertThat(content).contains("META-INF/services");
    }

    @Test
    void testProxyConfigContainsArchiveInterfaces() throws IOException {
        Path proxyConfig = Paths.get(NATIVE_IMAGE_DIR, "proxy-config.json");
        String content = Files.readString(proxyConfig);
        
        assertThat(content).contains("org.apache.commons.compress.archivers.ArchiveInputStream");
        assertThat(content).contains("org.apache.commons.compress.compressors.CompressorInputStream");
    }

    @Test
    void testNativeImageDirectoryStructure() {
        Path nativeImageDir = Paths.get(NATIVE_IMAGE_DIR);
        assertThat(nativeImageDir).exists();
        assertThat(nativeImageDir).isDirectory();
        
        try (Stream<Path> files = Files.list(nativeImageDir)) {
            List<String> fileNames = files
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();
            
            assertThat(fileNames).containsExactlyInAnyOrder(
                    "reflect-config.json",
                    "resource-config.json",
                    "proxy-config.json"
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to list native-image directory", e);
        }
    }

    @Test
    void testJsonFilesAreValid() throws IOException {
        Path reflectConfig = Paths.get(NATIVE_IMAGE_DIR, "reflect-config.json");
        Path resourceConfig = Paths.get(NATIVE_IMAGE_DIR, "resource-config.json");
        Path proxyConfig = Paths.get(NATIVE_IMAGE_DIR, "proxy-config.json");
        
        // Basic JSON validation - check for balanced brackets
        String reflectContent = Files.readString(reflectConfig);
        String resourceContent = Files.readString(resourceConfig);
        String proxyContent = Files.readString(proxyConfig);
        
        assertThat(countChar(reflectContent, '[')).isEqualTo(countChar(reflectContent, ']'));
        assertThat(countChar(reflectContent, '{')).isEqualTo(countChar(reflectContent, '}'));
        
        assertThat(countChar(resourceContent, '[')).isEqualTo(countChar(resourceContent, ']'));
        assertThat(countChar(resourceContent, '{')).isEqualTo(countChar(resourceContent, '}'));
        
        assertThat(countChar(proxyContent, '[')).isEqualTo(countChar(proxyContent, ']'));
        assertThat(countChar(proxyContent, '{')).isEqualTo(countChar(proxyContent, '}'));
    }
    
    private int countChar(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }
}
