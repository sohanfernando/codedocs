package com.sohan.codedocs.service;

import com.sohan.codedocs.enums.EmbeddingTaskType;

import java.util.List;

public interface EmbeddingClient {
    float[] embed(String text, EmbeddingTaskType taskType);
    List<float[]> embedBatch(List<String> texts, EmbeddingTaskType taskType);
    String modelId();
    int dimensions();
}
