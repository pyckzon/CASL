package com.casl.portfolio.models.operations;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class Operation {
    private UUID operationId;
    private String operationStatus;
    private List<OperationDetail> operationDetails;



}
