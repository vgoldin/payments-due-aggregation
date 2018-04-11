package com.mambu.examples.lambda.schedule.model;

import java.math.BigDecimal;

public class Repayment {
    private BigDecimal principalDue;
    private Object encodedKey;
    private String state;

    public void setPrincipalDue(BigDecimal principalDue) {
        this.principalDue = principalDue;
    }

    public BigDecimal getPrincipalDue() {
        return principalDue;
    }

    public Object getEncodedKey() {
        return encodedKey;
    }

    public void setEncodedKey(String encodedKey) {
        this.encodedKey = encodedKey;
    }

    public void setState(String state) {
        this.state = state;
    }
}
