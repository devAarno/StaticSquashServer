<!--
Copyright (C) 2026 Parkhomenko Stanislav
This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
To view a copy of this license, visit http://creativecommons.org
-->

# Test Archives for StaticSquashServer

## Overview

This directory contains minimal test archives (total ~7KB) designed for comprehensive unit testing of the StaticSquashServer project.

## Archives

| File | Format | Size | Description |
|------|--------|------|-------------|
| `test.zip` | ZIP | 2.5 KB | ZIP archive with test files |
| `test.tar.gz` | TAR.GZ | 2.6 KB | Gzip-compressed TAR archive |
| `test.tar.xz` | TAR.XZ | 2.0 KB | XZ-compressed TAR archive |

## Archive Contents

All archives contain identical logical structure:

```
/
├── index.html                          # Root HTML file
├── empty_file.txt                      # Zero-byte file (edge case)
├── file with spaces.txt                # Spaces in filename
├── special-chars_@#.txt                # Special characters
├── UPPERCASE.HTML                      # Case sensitivity test
├── .hidden_file                        # Hidden/dot file
├── css/
│   └── style.css
├── js/
│   ├── app.js
│   └── lib/
│       └── vendor.js                   # Depth 2
├── data/
│   ├── report.json
│   ├── large_dummy.bin                 # 64KB for streaming tests
│   └── nested/
│       └── deep/
│           └── level3.json             # Depth 3
├── images/
│   ├── logo.png                        # Minimal 1x1 PNG
│   └── icons/
│       └── small.svg
└── allure-report/
    ├── index.html
    ├── widgets/
    │   └── summary.json
    └── exporters/
        └── dashboard/
            └── dashboard.html
```

## Edge Cases Covered

1. **Empty files** - `empty_file.txt` (0 bytes)
2. **Spaces in filenames** - `file with spaces.txt`
3. **Special characters** - `special-chars_@#.txt`
4. **Case sensitivity** - `UPPERCASE.HTML` vs `index.html`
5. **Hidden files** - `.hidden_file`
6. **Deep nesting** - `data/nested/deep/level3.json` (3 levels)
7. **Large files** - `data/large_dummy.bin` (64KB) for streaming tests
8. **Empty directories** - `allure-report/exporters/dashboard/` (no files)
9. **Allure-like structure** - Realistic report layout

## Unit Tests

### ArchiveParserTest

Location: `src/test/java/ru/devaarno/staticsquashserver/archive/ArchiveParserTest.java`

Tests:
- ZIP parsing and entry enumeration
- Deep nesting resolution
- Empty file handling
- Files with spaces
- Hidden files
- Case-sensitive path matching
- Large file streaming
- Directory entry filtering
- Allure report structure

### MimeTypeResolverTest

Location: `src/test/java/ru/devaarno/staticsquashserver/mime/MimeTypeResolverTest.java`

Tests:
- HTML, CSS, JS, JSON MIME types
- Image types (PNG, JPEG, GIF, SVG)
- Text and XML types
- Unknown extensions → octet-stream
- Case-insensitive extension matching
- Multiple dots in filename

## Running Tests

```bash
mvn test
```

## Regenerating Archives

If you need to recreate the archives:

```bash
# Create source directory structure
mkdir -p test_data/archive_content
cd test_data/archive_content

# (Create files as shown in test_archive_design.md)

# Create archives
zip -r ../test.zip .
tar -czf ../test.tar.gz .
tar -cJf ../test.tar.xz .

# Cleanup
cd ..
rm -rf archive_content
```

## Usage in Tests

```java
class MyArchiveTest {
    
    @Test
    void testZipParsing() throws IOException {
        Path zipPath = Paths.get("test_data/test.zip");
        ArchiveParser parser = ArchiveParserFactory.create(zipPath);
        ArchiveDescriptor descriptor = parser.parse(zipPath);
        
        assertFalse(descriptor.entries().isEmpty());
        // ... more assertions
    }
}
```

## Total Size

- **Archives only**: ~7 KB
- **With test classes**: ~16 KB

Perfect for CI/CD and fast unit test execution.
