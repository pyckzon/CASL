package com.casl.portfolio.utils;

import com.casl.portfolio.models.request.PortfolioRequest;
import com.casl.portfolio.models.request.RequestBody;

public class PortfolioUtilities {

    public PortfolioRequest createPortfolioRequest(RequestBody requestBody) {
        PortfolioRequest portfolioRequest = new PortfolioRequest();
        portfolioRequest.setRequestId(java.util.UUID.randomUUID());
        portfolioRequest.setHeader(new com.casl.portfolio.models.request.RequestHeader());
        portfolioRequest.getHeader().setTransactionId(java.util.UUID.randomUUID());
        portfolioRequest.getHeader().setTestData(false);
        portfolioRequest.setBody(requestBody);
        return portfolioRequest;
    }
}
