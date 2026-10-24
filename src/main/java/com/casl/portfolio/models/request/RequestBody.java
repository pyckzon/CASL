package com.casl.portfolio.models.request;

import com.casl.portfolio.models.Users.User;
import com.casl.portfolio.models.operations.Operation;
import lombok.Data;import lombok.Getter;import lombok.Setter;

@Data
@Getter
@Setter
public class RequestBody {
    private User user;
    private Operation operation;

}
