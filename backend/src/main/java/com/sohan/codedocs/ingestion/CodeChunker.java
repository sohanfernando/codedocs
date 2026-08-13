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
public class CodeChunker implements Chunker{

    private final IngestionProperties props;

    @Override
    public List<RawChunk> chunk(ScannedFile file) {
        List<String> lines = file.lines();
        if (lines.isEmpty()) return List.of();

        int window = props.chunkWindowLines();
        int step = Math.max(1, window - props.chunkOverlapLines());
        List<RawChunk> chunks = new ArrayList<>();

        for (int start = 0; start < lines.size(); start += step) {
            int end = Math.min(start + window, lines.size());
            String content = String.join("\n", lines.subList(start, end));

            if (!content.isBlank()) {
                // 1-indexed: GitHub's #L40-L98 anchors are 1-based.
                chunks.add(new RawChunk(cap(content), start + 1, end));
            }
            if (end == lines.size()) break;
        }
        return chunks;
    }

    private String cap(String content) {
        int max = props.chunkMaxChars();
        return content.length() <= max ? content : content.substring(0, max);
    }
}
