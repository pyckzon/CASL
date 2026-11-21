package com.casl.users.models.users;

import lombok.Data;

@Data
public class User {
    private String userId;
    private String userAlias;
    private PersonalData personalData;
}
