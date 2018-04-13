package com.mambu.examples.lambda.stream.model;

import java.math.BigDecimal;

public class MambuRepaymentTransaction {
    private final String type = "REPAYMENT";
    private BigDecimal amount;

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
