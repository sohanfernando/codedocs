package com.sohan.codedocs.ingestion;

import com.sohan.codedocs.ingestion.model.ScannedFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChunkerFactory {

    private final CodeChunker codeChunker;
    private final MarkdownChunker markdownChunker;

    public Chunker forFile(ScannedFile file){
        return "md".equals(file.extension()) ? markdownChunker : codeChunker;
    }
}
