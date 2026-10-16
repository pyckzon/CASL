package com.casl.portfolio.models.operations;

import lombok.Data;

@Data
public class OperationDetail {
    private String operationType;
    private String operationDescription;
    private OperationResult<?> operationResult;
}
