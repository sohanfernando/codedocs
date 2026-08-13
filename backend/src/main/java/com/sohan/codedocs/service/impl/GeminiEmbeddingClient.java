package com.sohan.codedocs.service.impl;

import com.sohan.codedocs.config.properties.GeminiProperties;
import com.sohan.codedocs.enums.EmbeddingTaskType;
import com.sohan.codedocs.exception.AiProviderException;
import com.sohan.codedocs.service.EmbeddingClient;
import com.sohan.codedocs.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiEmbeddingClient implements EmbeddingClient {

    private final GeminiApiCaller apiCaller;
    private final GeminiProperties props;

    @Override
    public float[] embed(String text, EmbeddingTaskType taskType) {
        return toNormalisedArray(apiCaller.embedOne(text, taskType));
    }

    /**
     * gemini-embedding-001 no longer supports the synchronous batchEmbedContents
     * method, so this issues one embedContent call per text. The interface is
     * unchanged, so no caller had to be modified.
     */
    @Override
    public List<float[]> embedBatch(List<String> texts, EmbeddingTaskType taskType) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            vectors.add(toNormalisedArray(apiCaller.embedOne(text, taskType)));
        }
        return vectors;
    }

    @Override
    public String modelId() {
        return props.embeddingModel();
    }

    @Override
    public int dimensions() {
        return props.embeddingDimensions();
    }

    private float[] toNormalisedArray(List<Double> values) {
        if (values.size() != props.embeddingDimensions()) {
            throw new AiProviderException("Expected %d dimensions, received %d"
                    .formatted(props.embeddingDimensions(), values.size()), null);
        }
        float[] raw = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            raw[i] = values.get(i).floatValue();
        }
        // Critical: at reduced dimensionality the vectors are not pre-normalised,
        // and cosine distance on unnormalised vectors ranks partly by magnitude.
        return VectorUtils.normalise(raw);
    }
}
