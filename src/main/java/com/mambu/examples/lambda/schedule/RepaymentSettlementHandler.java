package com.mambu.examples.lambda.schedule;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.Map;

public class RepaymentSettlementHandler implements RequestHandler<Map<String, Object>, Object> {
    @Override
    public Object handleRequest(Map<String, Object> stringObjectMap, Context context) {
        /*
        1. Get Pending payments from DynamoDB
        2. Settle Payment
        3. update the status
         */
        return null;
    }
}
