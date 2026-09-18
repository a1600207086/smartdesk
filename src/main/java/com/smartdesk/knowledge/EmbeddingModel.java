package com.smartdesk.knowledge;

import java.util.List;

public interface EmbeddingModel {

    String modelId();

    int dimensions();

    float[] embed(String text);

    default List<float[]> embedAll(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }
}