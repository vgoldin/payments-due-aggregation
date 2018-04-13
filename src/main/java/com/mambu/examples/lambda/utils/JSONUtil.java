package com.mambu.examples.lambda.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;

public class JSONUtil {
    public static MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static String serializeToJSON(Object o) {
        ObjectMapper mapper = new ObjectMapper();
        String json;
        try {
            json = mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return json;
    }
}
