package com.casl.portfolio.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioController.class)
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPortfolioReturnsPortfolioAsJson() throws Exception {
        mockMvc.perform(get("/rest-api/v1/crud/user/{userId}", "ana")
                        .param("id", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("ana"))
                .andExpect(jsonPath("$.requestId").value(123))
                .andExpect(jsonPath("$.message").value("Portfolio fetched successfully"));
    }

    // TODO: Add tests for request validation, error responses, and the other controller endpoints.
}
