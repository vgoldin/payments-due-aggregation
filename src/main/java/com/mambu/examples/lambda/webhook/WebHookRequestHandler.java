package com.mambu.examples.lambda.webhook;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Map;

public class WebHookRequestHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {
    private static final Logger LOG = Logger.getLogger(WebHookRequestHandler.class);
    public static final String TABLE_NAME = "Repayments";

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        JsonNode rootNode = new ObjectMapper().valueToTree(input);
        storeRepayment(rootNode);

        return ApiGatewayResponse.builder()
                .setStatusCode(202)
                .build();
    }

    private void storeRepayment(JsonNode rootNode) {
        String encodedKey = rootNode.path("encodedKey").asText();
        String status = rootNode.path("state").asText();

        LOG.info("encodedKey:" + encodedKey);
        LOG.info("state:" + status);

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDB dynamoDB = new DynamoDB(client);

        Table table = dynamoDB.getTable(TABLE_NAME);
        table.putItem(new Item()
                .withPrimaryKey("encodedKey", encodedKey)
                .withString("encodedKey", encodedKey)
                .withString("state", status));
    }
}
