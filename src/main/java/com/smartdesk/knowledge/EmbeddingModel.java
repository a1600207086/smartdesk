package com.smartdesk.knowledge;

public interface EmbeddingModel {
    float[] embed(String text);
    int dimensions();
}