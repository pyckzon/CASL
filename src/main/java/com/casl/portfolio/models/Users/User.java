package com.casl.portfolio.models.Users;

import lombok.Data;

@Data
public class User {
    private String userId;
    private String userAlias;
    private PersonalData personalData;
}
