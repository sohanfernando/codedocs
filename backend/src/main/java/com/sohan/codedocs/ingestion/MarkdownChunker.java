package com.sohan.codedocs.ingestion;

import com.sohan.codedocs.config.properties.IngestionProperties;
import com.sohan.codedocs.ingestion.model.RawChunk;
import com.sohan.codedocs.ingestion.model.ScannedFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MarkdownChunker implements Chunker{
    private final CodeChunker fallback;
    private final IngestionProperties props;

    @Override
    public List<RawChunk> chunk(ScannedFile file) {
        List<String> lines = file.lines();
        if (lines.isEmpty()) return List.of();

        List<RawChunk> chunks = new ArrayList<>();
        int sectionStart = 0;
        boolean inFence = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.trim().startsWith("```")) {
                inFence = !inFence;     // never split inside a fenced code block
                continue;
            }
            if (!inFence && isHeading(line) && i > sectionStart) {
                addSection(chunks, file, lines, sectionStart, i);
                sectionStart = i;
            }
        }
        addSection(chunks, file, lines, sectionStart, lines.size());
        return chunks;
    }

    private boolean isHeading(String line) {
        return line.startsWith("# ") || line.startsWith("## ");
    }

    private void addSection(List<RawChunk> chunks, ScannedFile file,
                            List<String> lines, int start, int end) {
        String content = String.join("\n", lines.subList(start, end));
        if (content.isBlank()) return;

        if (content.length() <= props.chunkMaxChars()) {
            chunks.add(new RawChunk(content, start + 1, end));
            return;
        }
        // Oversized section — re-window it, shifting line numbers back to absolute.
        ScannedFile section = new ScannedFile(
                file.relativePath(), file.extension(), file.language(),
                lines.subList(start, end));
        for (RawChunk sub : fallback.chunk(section)) {
            chunks.add(new RawChunk(sub.content(),
                    sub.startLine() + start, sub.endLine() + start));
        }
    }
}
