package com.casl.portfolio.utils;

import com.casl.portfolio.models.Users.PersonalData;
import com.casl.portfolio.models.Users.User;
import com.casl.portfolio.models.operations.Operation;
import com.casl.portfolio.models.operations.OperationDetail;
import com.casl.portfolio.models.operations.OperationResult;
import com.casl.portfolio.models.request.PortfolioRequest;
import com.casl.portfolio.models.request.RequestBody;
import com.casl.portfolio.models.request.RequestHeader;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PortfolioUtilities {

    public static final String[] FAKE_OPERATION_TYPES = {"CREATE", "VALIDATE"};

    public PortfolioRequest createFakePortfolioRequest() {
        PortfolioRequest portfolioRequest = new PortfolioRequest();
        portfolioRequest.setRequestId(UUID.randomUUID());
        portfolioRequest.setCreatedAt(Instant.now());
        portfolioRequest.setHeader(createFakeRequestHeader());
        portfolioRequest.setBody(createFakeRequestBody());
        return portfolioRequest;
    }

    public RequestHeader createFakeRequestHeader() {
        RequestHeader header = new RequestHeader();
        header.setTransactionId(UUID.randomUUID());
        header.setTestData(true);
        return header;
    }

    public RequestBody createFakeRequestBody() {
        RequestBody body = new RequestBody();
        body.setUser(createFakeUser());
        body.setOperation(createFakeOperation());
        return body;
    }

    public User createFakeUser() {
        User user = new User();
        user.setUserId("usr-" + UUID.randomUUID().toString().substring(0, 8));
        user.setUserAlias("fake_user_" + UUID.randomUUID().toString().substring(0, 5));
        user.setPersonalData(createFakePersonalData());
        return user;
    }

    public PersonalData createFakePersonalData() {
        PersonalData personalData = new PersonalData();
        personalData.setFirstName("Juan");
        personalData.setLastName("Pérez");
        personalData.setEmail("juan.perez@example.com");
        personalData.setPhoneNumber("+34-600-123-456");
        personalData.setSensitiveData("sensitive-" + UUID.randomUUID().toString().substring(0, 8));
        return personalData;
    }

    public Operation createFakeOperation() {
        Operation operation = new Operation();
        operation.setOperationId(UUID.randomUUID());
        operation.setOperationStatus("IN_PROGRESS");
        operation.setOperationDetails(List.of(createFakeOperationDetail(FAKE_OPERATION_TYPES[0]), createFakeOperationDetail(FAKE_OPERATION_TYPES[1])));
        return operation;
    }

    public OperationDetail createFakeOperationDetail(String operationType) {
        OperationDetail detail = new OperationDetail();
        detail.setOperationType(operationType);
        detail.setOperationDescription("Descripción de " + operationType + " generada con datos ficticios");
        detail.setOperationResult(createFakeOperationResult());
        return detail;
    }

    public OperationResult<Object> createFakeOperationResult() {
        OperationResult<Object> operationResult = new OperationResult<>();
        operationResult.setSuccess(true);
        operationResult.setMessage("Operación " + UUID.randomUUID().toString().substring(0, 8) + " completada correctamente");
        operationResult.setResultData("fake-result-" + UUID.randomUUID().toString().substring(0, 8));
        return operationResult;
    }

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

