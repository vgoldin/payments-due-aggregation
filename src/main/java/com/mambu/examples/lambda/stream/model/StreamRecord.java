package com.mambu.examples.lambda.stream.model;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.Map;

public class StreamRecord {
    private java.util.Map<String, AttributeValue> keys;
    private java.util.Map<String, AttributeValue> newImage;

    public Map<String, AttributeValue> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, AttributeValue> keys) {
        this.keys = keys;
    }

    public Map<String, AttributeValue> getNewImage() {
        return newImage;
    }

    public void setNewImage(Map<String, AttributeValue> newImage) {
        this.newImage = newImage;
    }
}
