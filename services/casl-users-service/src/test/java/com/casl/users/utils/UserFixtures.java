package com.casl.users.utils;

import com.casl.users.models.users.PersonalData;
import com.casl.users.models.users.User;
import com.casl.users.models.request.PortfolioRequest;
import com.casl.users.models.request.RequestBody;
import com.casl.users.models.request.RequestHeader;

import java.time.Instant;
import java.util.UUID;

public class UserFixtures {

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
}
