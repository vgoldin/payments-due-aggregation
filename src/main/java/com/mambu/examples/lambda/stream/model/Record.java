package com.mambu.examples.lambda.stream.model;

public class Record {
    private StreamRecord dynamodb;

    public StreamRecord getDynamodb() {
        return this.dynamodb;
    }

    public void setDynamodb(StreamRecord dynamodb) {
        this.dynamodb = dynamodb;
    }
}
