package com.casl.users.models.request;

import lombok.Data;

import java.util.UUID;

@Data
public class RequestHeader {
    private UUID transactionId;
    private boolean isTestData;

}
