package com.casl.portfolio.models.request;

import lombok.Data;

import java.util.UUID;

@Data
public class RequestHeader {
    private UUID transactionId;
    private boolean isTestData;

}
