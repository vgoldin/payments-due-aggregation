package com.mambu.examples.lambda.webhook;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class WebHookRequestHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {
    private static final Logger LOG = Logger.getLogger(WebHookRequestHandler.class);
    public static final String TABLE_NAME = "Repayments";

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        JsonNode rootNode;
        try {
            rootNode = new ObjectMapper().readTree((String) input.get("body"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ApiGatewayResponse response;
        try {
            storeRepayment(rootNode);

            response = ApiGatewayResponse.builder()
                    .setStatusCode(202)
                    .build();
        } catch (Throwable e) {
            LOG.error(e.getMessage() + "; payload: " + input, e);

            response = ApiGatewayResponse.builder()
                    .setStatusCode(500)
                    .setObjectBody(e.getMessage())
                    .build();
        }

        return response;
    }

    private void storeRepayment(JsonNode rootNode) {
        String encodedKey = rootNode.path("encodedKey").asText();
        String status = rootNode.path("state").asText();

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDB dynamoDB = new DynamoDB(client);

        Table table = dynamoDB.getTable(TABLE_NAME);
        table.putItem(
            new Item()
                .withPrimaryKey("encodedKey", encodedKey)
                .withString("state", status)
                .withJSON("repayment", prettyPrintJsonString(rootNode))
            );
    }

    public String prettyPrintJsonString(JsonNode jsonNode) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object json = mapper.readValue(jsonNode.toString(), Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
