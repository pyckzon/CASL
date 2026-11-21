package com.casl.users.models.request;

import com.casl.users.models.users.User;
import lombok.Data;

@Data
public class RequestBody {
    private User user;
}
