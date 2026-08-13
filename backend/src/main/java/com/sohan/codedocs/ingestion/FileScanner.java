package com.sohan.codedocs.ingestion;

import com.sohan.codedocs.config.properties.IngestionProperties;
import com.sohan.codedocs.exception.IngestionException;
import com.sohan.codedocs.ingestion.model.ScannedFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileScanner {
    private final FileFilter fileFilter;
    private final IngestionProperties props;

    public List<ScannedFile> scan(Path root) {
        List<ScannedFile> files = new ArrayList<>();
        long totalBytes = 0;

        // Files.walk does not follow symlinks by default — keep it that way.
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.toList()) {
                if (files.size() >= props.maxFilesPerRepo()) {
                    log.warn("File limit {} reached; truncating scan", props.maxFilesPerRepo());
                    break;
                }
                if (!fileFilter.shouldIndex(root, path)) continue;

                long size = Files.size(path);
                if (totalBytes + size > props.maxTotalBytes()) {
                    log.warn("Total byte budget reached; truncating scan");
                    break;
                }

                readFile(root, path).ifPresent(scanned -> files.add(scanned));
                totalBytes += size;
            }
        } catch (IOException | UncheckedIOException e) {
            throw new IngestionException("Failed to scan repository contents", e);
        }
        return files;
    }

    private java.util.Optional<ScannedFile> readFile(Path root, Path path) {
        String relativePath = root.relativize(path).toString().replace('\\', '/');
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String fileName = path.getFileName().toString();
            return java.util.Optional.of(new ScannedFile(
                    relativePath,
                    fileFilter.extensionOf(fileName.toLowerCase()),
                    fileFilter.languageFor(fileName),
                    lines));
        } catch (MalformedInputException e) {
            return java.util.Optional.empty();   // not valid UTF-8 — skip
        } catch (IOException e) {
            log.debug("Skipping unreadable file {}", relativePath);
            return java.util.Optional.empty();
        }
    }
}
