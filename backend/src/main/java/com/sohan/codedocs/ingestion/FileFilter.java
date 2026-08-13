package com.sohan.codedocs.ingestion;

import com.sohan.codedocs.config.properties.IngestionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FileFilter {
    private static final Set<String> EXCLUDED_DIRS = Set.of(
            ".git", ".github", "node_modules", "target", "build", "out", "bin",
            "dist", "vendor", ".idea", ".vscode", "__pycache__", ".next",
            "coverage", ".gradle", ".mvn", "venv", ".venv");

    private static final Set<String> EXCLUDED_FILENAMES = Set.of(
            "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
            "composer.lock", "poetry.lock", "gradle.lockfile", "cargo.lock");

    /** Allowlist, not blocklist. A blocklist always misses something. */
    private static final Map<String, String> EXTENSION_TO_LANGUAGE = Map.ofEntries(
            Map.entry("java", "java"), Map.entry("kt", "kotlin"),
            Map.entry("js", "javascript"), Map.entry("jsx", "jsx"),
            Map.entry("ts", "typescript"), Map.entry("tsx", "tsx"),
            Map.entry("py", "python"), Map.entry("go", "go"),
            Map.entry("rb", "ruby"), Map.entry("php", "php"),
            Map.entry("cs", "csharp"), Map.entry("cpp", "cpp"),
            Map.entry("c", "c"), Map.entry("h", "c"),
            Map.entry("rs", "rust"), Map.entry("sql", "sql"),
            Map.entry("yml", "yaml"), Map.entry("yaml", "yaml"),
            Map.entry("md", "markdown"), Map.entry("json", "json"),
            Map.entry("xml", "xml"), Map.entry("csproj", "xml"),
            Map.entry("gradle", "gradle"), Map.entry("properties", "properties"),
            Map.entry("sh", "bash"), Map.entry("html", "html"),
            Map.entry("css", "css")
    );

    private static final int BINARY_PROBE_BYTES = 8192;

    private final IngestionProperties props;

    public boolean shouldIndex(Path root, Path candidate) {
        try {
            // Path traversal / symlink escape: a repository can contain a symlink
            // pointing outside the clone. Resolve it and confirm containment.
            Path real = candidate.toRealPath();
            if (!real.startsWith(root.toRealPath())) {
                return false;
            }
            if (!Files.isRegularFile(real)) return false;
            if (hasExcludedSegment(root.relativize(real))) return false;

            String fileName = real.getFileName().toString().toLowerCase(Locale.ROOT);
            if (EXCLUDED_FILENAMES.contains(fileName)) return false;
            if (!EXTENSION_TO_LANGUAGE.containsKey(extensionOf(fileName))) return false;
            if (Files.size(real) > props.maxFileBytes()) return false;

            return !isBinary(real);

        } catch (IOException e) {
            return false;   // unreadable or broken symlink — skip quietly
        }
    }

    public String languageFor(String fileName) {
        return EXTENSION_TO_LANGUAGE.getOrDefault(
                extensionOf(fileName.toLowerCase(Locale.ROOT)), "text");
    }

    public String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1);
    }

    private boolean hasExcludedSegment(Path relative) {
        for (Path segment : relative) {
            if (EXCLUDED_DIRS.contains(segment.toString())) return true;
        }
        return false;
    }

    /** A null byte in the first 8 KB means binary. Catches minified bundles too. */
    private boolean isBinary(Path path) throws IOException {
        byte[] probe = new byte[BINARY_PROBE_BYTES];
        try (InputStream in = Files.newInputStream(path)) {
            int read = in.read(probe);
            for (int i = 0; i < read; i++) {
                if (probe[i] == 0) return true;
            }
        }
        return false;
    }
}
