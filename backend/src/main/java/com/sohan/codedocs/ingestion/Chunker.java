package com.sohan.codedocs.ingestion;

import com.sohan.codedocs.ingestion.model.RawChunk;
import com.sohan.codedocs.ingestion.model.ScannedFile;

import java.util.List;

public interface Chunker {
    List<RawChunk> chunk(ScannedFile file);
}
