package com.casl.portfolio.models.request;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class PortfolioRequest {
    private UUID requestId;
    private Instant createdAt = Instant.now();
    private RequestHeader header;
    private RequestBody body;

}
