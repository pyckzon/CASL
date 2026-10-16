package com.casl.portfolio.models.operations;

import lombok.Data;

@Data
public class OperationResult<T> {
    private boolean success;
    private String message;
    private T resultData;

}
