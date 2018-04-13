package com.mambu.examples.lambda.schedule;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.mambu.examples.lambda.schedule.model.Repayment;
import com.mambu.examples.lambda.utils.JSONUtil;
import com.mambu.examples.lambda.webhook.WebHookRequestHandler;
import okhttp3.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class RepaymentSettlementHandler implements RequestHandler<Map<String, Object>, Object> {
    private static final Logger LOG = Logger.getLogger(WebHookRequestHandler.class);
    private String TABLE_NAME;
    private DynamoDB dynamoDB;
    private AmazonDynamoDB client;

    @Override
    public Object handleRequest(Map<String, Object> stringObjectMap, Context context) {
        initDynamoDB(context);

        List<Repayment> repaymentList = retrievePendingRepayments();
        repaymentList.forEach(o -> settleRePayment(o));

        return null;
    }

    private void initDynamoDB(Context context) {
        TABLE_NAME = System.getenv("TABLE_NAME");

        client = AmazonDynamoDBClientBuilder.defaultClient();
        dynamoDB = new DynamoDB(client);
    }

    private void settleRePayment(Repayment o) {
        settleRepaymentWithPaymentProvider(o);
        updateRepaymentStatus(o);
    }

    private void settleRepaymentWithPaymentProvider(Repayment o) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(System.getenv("PAYMENT_URI"))
                .post(RequestBody.create(JSONUtil.JSON, JSONUtil.serializeToJSON(o)))
                .build();

        try {
            Response response = client.newCall(request).execute();

            LOG.debug(response.body().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateRepaymentStatus(Repayment o) {
        Table table = dynamoDB.getTable(TABLE_NAME);
        UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("encodedKey", o.getEncodedKey())
                .withUpdateExpression("set #s = :s")
                .withNameMap(new NameMap().with("#s", "state"))
                .withValueMap(new ValueMap().with(":s", "SETTLED"))
                .withReturnValues(ReturnValue.UPDATED_NEW);

        table.updateItem(updateItemSpec);
    }

    private List<Repayment> retrievePendingRepayments() {
        Map<String, AttributeValue> expressionAttributeValues =
                new HashMap<>();
        expressionAttributeValues.put(":s", new AttributeValue().withS("PENDING"));

        ScanRequest scanRequest = new ScanRequest()
                .withTableName(TABLE_NAME)
                .withFilterExpression("#s = :s")
                .withExpressionAttributeNames(Collections.singletonMap("#s", "state"))
                .withExpressionAttributeValues(expressionAttributeValues);

        ScanResult result = client.scan(scanRequest);

        return transform(result);
    }

    private List<Repayment> transform(ScanResult result) {
        List<Repayment> repayments = new ArrayList<>();

        for (Map<String, AttributeValue> item : result.getItems()){
            repayments.add(mapRepayment(item));
        }

        return repayments;
    }

    private Repayment mapRepayment(Map<String, AttributeValue> item) {
        Repayment r  = new Repayment();
        r.setEncodedKey(item.get("encodedKey").getS());

        BigDecimal principalDue = new BigDecimal(item.get("repayment").getM().get("principalDue").getS());
        r.setPrincipalDue(principalDue);
        r.setState(item.get("state").getS());

        return r;
    }


}
