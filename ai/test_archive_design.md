<!--
Copyright (C) 2026 Parkhomenko Stanislav
This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
To view a copy of this license, visit http://creativecommons.org
-->

# Test Archive Structure for StaticSquashServer Unit Tests

## Overview
This document describes a minimal test archive structure (few KB) designed for comprehensive unit testing of tree structure handling, edge cases, and common errors.

## Archive Formats to Test
- `test.zip` - ZIP format
- `test.tar.gz` - TAR.GZ format  
- `test.tar.xz` - TAR.XZ format

## Directory Structure (logical, applies to all formats)

```
/
├── index.html                          # Root level file
├── css/                                # Empty directory (edge case)
│   └── .gitkeep                        # Placeholder to create dir
├── css/style.css                       # Regular file
├── js/
│   ├── app.js                          # Regular file
│   └── lib/
│       └── vendor.js                   # Nested file (depth 2)
├── data/
│   ├── report.json                     # JSON file (Allure-like)
│   └── nested/
│       └── deep/
│           └── level3.json             # Deep nesting (depth 3)
├── images/
│   ├── logo.png                        # Binary-like file
│   └── icons/
│       └── small.svg                   # SVG file
├── empty_file.txt                      # Zero-byte file (edge case)
├── file with spaces.txt                # Spaces in name (edge case)
├── special-chars_@#.txt                # Special characters (edge case)
├── UPPERCASE.HTML                      # Case sensitivity test
└── .hidden_file                        # Hidden file (dotfile)
```

## Edge Cases Covered

### 1. Empty Directory Handling
- `css/` directory with only `.gitkeep` placeholder
- Tests: directory entries are skipped, parent dirs work correctly

### 2. Zero-Byte Files
- `empty_file.txt` - 0 bytes
- Tests: Content-Length header, stream handling for empty content

### 3. Deep Nesting
- `data/nested/deep/level3.json` - 3 levels deep
- Tests: path traversal, route registration for nested paths

### 4. Special Characters in Filenames
- `file with spaces.txt` - spaces
- `special-chars_@#.txt` - special chars
- Tests: URL encoding/decoding, path normalization

### 5. Case Sensitivity
- `UPPERCASE.HTML` vs `index.html`
- Tests: case-sensitive path matching

### 6. Hidden Files
- `.hidden_file` - dotfile at root
- Tests: dotfile handling, hidden directory handling

### 7. Allure-like Structure
```
allure-report/
├── index.html                          # Main report page
├── exporters/
│   └── dashboard/
│       └── dashboard.html              # Dashboard view
├── widgets/
│   ├── summary.json                    # Summary widget data
│   ├── behaviors.json                  # Behaviors widget
│   └── categories.json                 # Categories widget
├── graph.js                            # Chart.js library
└── charts/
    └── timeline.json                   # Timeline data
```

### 8. Large File Simulation (for streaming tests)
- `data/large_dummy.bin` - 64KB of repeated pattern
- Tests: streaming without full memory load, chunked reading

### 9. Path Traversal Attempts (security)
- Files with names like `../escape.txt` should NOT be created
- Tests: path validation, security against directory traversal

### 10. Duplicate Names (case variations)
- `readme.txt` and `README.TXT` in same directory
- Tests: case-sensitive filesystem handling

## Minimal File Contents

### index.html (root)
```html
<!DOCTYPE html>
<html><head><title>Test</title></head>
<body><h1>Root Index</h1></body></html>
```

### css/style.css
```css
body { margin: 0; padding: 20px; }
```

### js/app.js
```javascript
console.log('App loaded');
```

### js/lib/vendor.js
```javascript
var Vendor = { version: '1.0.0' };
```

### data/report.json
```json
{"tests": 42, "passed": 40, "failed": 2}
```

### data/nested/deep/level3.json
```json
{"level": 3, "deep": true}
```

### images/logo.png
```
PNG placeholder - 8 bytes header + minimal IHDR + IEND
```
(Use actual minimal PNG: 67 bytes)

### images/icons/small.svg
```xml
<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16">
  <circle cx="8" cy="8" r="4"/>
</svg>
```

### empty_file.txt
```
(empty - 0 bytes)
```

### file with spaces.txt
```
File with spaces in name
```

### special-chars_@#.txt
```
Special characters test
```

### UPPERCASE.HTML
```html
<html><body>UPPERCASE</body></html>
```

### .hidden_file
```
Hidden file content
```

### allure-report/* (Allure-like)
Similar minimal HTML/JSON structures as above.

### data/large_dummy.bin
```
64KB of pattern: "LINE_NUMBER: Some test data content\n" repeated
```

## Test Scenarios

### Unit Test Cases

1. **Archive Parsing**
   - Parse ZIP archive successfully
   - Parse TAR.GZ archive successfully
   - Parse TAR.XZ archive successfully
   - Detect unsupported format
   - Handle corrupted archive header

2. **Entry Discovery**
   - List all files (excluding directories)
   - Skip empty directories
   - Include hidden files
   - Handle deep nesting

3. **Path Resolution**
   - Resolve root-level file
   - Resolve nested file (depth 1)
   - Resolve deeply nested file (depth 3+)
   - Handle path with spaces
   - Handle special characters
   - Case-sensitive matching

4. **Content Retrieval**
   - Read regular file content
   - Read zero-byte file
   - Read large file via streaming
   - Verify Content-Type header mapping

5. **Error Handling**
   - 404 for non-existent file
   - 404 for directory path
   - Handle corrupted entry data
   - Stream closure on error

6. **Media Type Detection**
   - `.html` → text/html
   - `.css` → text/css
   - `.js` → application/javascript
   - `.json` → application/json
   - `.png` → image/png
   - `.svg` → image/svg+xml
   - `.txt` → text/plain
   - unknown → application/octet-stream

7. **Concurrent Access**
   - Multiple requests to same file
   - Multiple requests to different files
   - Queue merging for duplicate requests

8. **Security**
   - Reject path traversal attempts
   - Validate entry names
   - Handle symlinks (if present)

## Archive Creation Script (Bash)

```bash
#!/bin/bash
# create_test_archives.sh

TEST_DIR="test_archive_content"
mkdir -p "$TEST_DIR"

# Create directory structure
mkdir -p "$TEST_DIR/css"
mkdir -p "$TEST_DIR/js/lib"
mkdir -p "$TEST_DIR/data/nested/deep"
mkdir -p "$TEST_DIR/images/icons"
mkdir -p "$TEST_DIR/allure-report/exporters/dashboard"
mkdir -p "$TEST_DIR/allure-report/widgets"
mkdir -p "$TEST_DIR/allure-report/charts"

# Create files with content
echo '<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Root Index</h1></body></html>' > "$TEST_DIR/index.html"
echo 'body { margin: 0; padding: 20px; }' > "$TEST_DIR/css/style.css"
echo "console.log('App loaded');" > "$TEST_DIR/js/app.js"
echo 'var Vendor = { version: "1.0.0" };' > "$TEST_DIR/js/lib/vendor.js"
echo '{"tests": 42, "passed": 40, "failed": 2}' > "$TEST_DIR/data/report.json"
echo '{"level": 3, "deep": true}' > "$TEST_DIR/data/nested/deep/level3.json"
echo '{"tests": 42, "passed": 40, "failed": 2}' > "$TEST_DIR/allure-report/index.html"
echo '{"summary": "Allure report data"}' > "$TEST_DIR/allure-report/widgets/summary.json"
echo '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><circle cx="8" cy="8" r="4"/></svg>' > "$TEST_DIR/images/icons/small.svg"
touch "$TEST_DIR/empty_file.txt"
echo 'File with spaces in name' > "$TEST_DIR/file with spaces.txt"
echo 'Special characters test' > "$TEST_DIR/special-chars_@#.txt"
echo '<html><body>UPPERCASE</body></html>' > "$TEST_DIR/UPPERCASE.HTML"
echo 'Hidden file content' > "$TEST_DIR/.hidden_file"
dd if=/dev/zero bs=1024 count=64 2>/dev/null | tr '\0' 'A' > "$TEST_DIR/data/large_dummy.bin"

# Create minimal PNG (1x1 red pixel)
printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82' > "$TEST_DIR/images/logo.png"

# Create ZIP archive
cd "$TEST_DIR" && zip -r ../test.zip . && cd ..

# Create TAR.GZ archive
tar -czf test.tar.gz -C . "$TEST_DIR"

# Create TAR.XZ archive
tar -cJf test.tar.xz -C . "$TEST_DIR"

# Cleanup
rm -rf "$TEST_DIR"

echo "Archives created: test.zip, test.tar.gz, test.tar.xz"
```

## Expected Archive Sizes

| Format    | Size (approx) |
|-----------|---------------|
| ZIP       | ~3-5 KB       |
| TAR.GZ    | ~3-5 KB       |
| TAR.XZ    | ~2-4 KB       |

## Java Test Data Generator

For tests that need to create archives programmatically:

```java
/**
 * Creates test archives in memory for unit testing.
 * Uses Apache Commons Compress to generate archives without filesystem.
 */
public class TestArchiveGenerator {
    
    /**
     * Creates a minimal ZIP archive with test structure.
     * Returns archive as byte array.
     */
    public static byte[] createTestZip() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(baos)) {
            // Add index.html
            addEntry(zos, "index.html", "<html><body>Test</body></html>");
            // Add nested file
            addEntry(zos, "data/nested/file.json", "{\"key\": \"value\"}");
            // Add empty file
            addEntry(zos, "empty.txt", "");
            // Add file with spaces
            addEntry(zos, "file with spaces.txt", "content");
        }
        return baos.toByteArray();
    }
    
    private static void addEntry(ZipArchiveOutputStream zos, String name, String content) 
            throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(name);
        zos.putArchiveEntry(entry);
        if (content != null) {
            zos.write(content.getBytes(StandardCharsets.UTF_8));
        }
        zos.closeArchiveEntry();
    }
}
```

## Summary

This test archive structure provides:
- **Minimal size**: ~2-3 KB per archive (test.zip: 2.5KB, test.tar.gz: 2.6KB, test.tar.xz: 2.0KB)
- **Comprehensive coverage**: 16 files across 4 nesting levels
- **Edge cases**: empty files, spaces, special chars, hidden files, case sensitivity
- **Allure simulation**: realistic report structure with widgets and exporters
- **Multiple formats**: ZIP, TAR.GZ, TAR.XZ
- **Security testing**: path traversal prevention
- **Streaming tests**: 64KB file for memory efficiency validation

## Actual Test Files Location

Archives are located in: `test_data/` directory:
- `test_data/test.zip` - ZIP format (2.5 KB)
- `test_data/test.tar.gz` - TAR.GZ format (2.6 KB)
- `test_data/test.tar.xz` - TAR.XZ format (2.0 KB)

See `test_data/README.md` for detailed usage instructions.

## Unit Tests

Test classes:
- `src/test/java/ru/devaarno/staticsquashserver/archive/ArchiveParserTest.java` - Archive parsing tests
- `src/test/java/ru/devaarno/staticsquashserver/mime/MimeTypeResolverTest.java` - MIME type resolution tests

Run tests with: `mvn test`
