package com.mambu.examples.lambda.stream;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Record;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordObjectMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.mambu.examples.lambda.stream.model.MambuRepaymentTransaction;
import com.mambu.examples.lambda.utils.JSONUtil;
import okhttp3.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RepaymentSettlementToMambuHandler implements RequestHandler<Map<String, Object>, Object> {
    private static final Logger LOG = Logger.getLogger(RepaymentSettlementToMambuHandler.class);
    public static String TABLE_NAME;
    private DynamoDB dynamoDB;
    private AmazonDynamoDB client;

    @Override
    public Object handleRequest(Map<String, Object> input, Context context) {
        initDynamoDB(context);

        List<Record> records = parseRecords(input);

        for (Record r : records) {
            Map<String, AttributeValue> image = r.getDynamodb().getNewImage();
            String state  = image.get("state").getS();

            if (state != null && state.equals("SETTLED")) {
                Map<String, AttributeValue> repayment = image.get("repayment").getM();

                MambuRepaymentTransaction transaction = new MambuRepaymentTransaction();
                transaction.setAmount(new BigDecimal(repayment.get("principalDue").getS()));

                String accountEncodedKey = repayment.get("parentAccountKey").getS();
                settleRepaymentWithMambu(accountEncodedKey, transaction);

                String encodedKey = r.getDynamodb().getKeys().get("encodedKey").getS();
                deleteStagedRepayment(encodedKey);
            }
        }

        return null;
    }

    private List<Record> parseRecords(Map<String, Object> input) {
        List<Record> records;
        try {
            RecordObjectMapper om = new RecordObjectMapper();
            SimpleModule module = new SimpleModule("aws-dynamodb", Version.unknownVersion());
            module.addSerializer(Date.class, DateSerializer.instance);
            module.addDeserializer(Date.class, new DateDeserializers.DateDeserializer());

            om.registerModule(module);

            om.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            String JSON = om.writeValueAsString(input.get("Records"));
            records = om.readValue(JSON, new TypeReference<List<Record>>() {});

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return records;
    }

    private void initDynamoDB(Context context) {
        TABLE_NAME = System.getenv("TABLE_NAME");

        client = AmazonDynamoDBClientBuilder.defaultClient();
        dynamoDB = new DynamoDB(client);
    }

    private void deleteStagedRepayment(String encodedKey) {
        Table table = dynamoDB.getTable(TABLE_NAME);
        UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("encodedKey", encodedKey)
                .withUpdateExpression("set #s = :s")
                .withNameMap(new NameMap().with("#s", "state"))
                .withValueMap(new ValueMap().with(":s", "DELETED")) // TODO: should actually delete in the future
                .withReturnValues(ReturnValue.UPDATED_NEW);

        table.updateItem(updateItemSpec);
    }


    private void settleRepaymentWithMambu(String accountEncodedKey, MambuRepaymentTransaction o) {
        String uri = System.getenv("MAMBU_REPAYMENT_URI");

        OkHttpClient client = new OkHttpClient();
        HttpUrl url = new HttpUrl.Builder()
                .host(uri)
                .addPathSegment("loans")
                .addPathSegment(accountEncodedKey)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(JSONUtil.JSON, JSONUtil.serializeToJSON(o)))
                .build();

        try {
            Response response = client.newCall(request).execute();

            LOG.debug(response.body().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public class UnixTimestampDeserializer extends JsonDeserializer<Date> {

        @Override
        public Date deserialize(JsonParser parser, DeserializationContext context)
                throws IOException, JsonProcessingException {
            String unixTimestamp = parser.getText().trim();
            return new Date(TimeUnit.SECONDS.toMillis(Long.valueOf(unixTimestamp)));
        }
    }

}
