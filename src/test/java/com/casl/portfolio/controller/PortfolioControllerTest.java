package com.casl.portfolio.controller;

import com.casl.portfolio.models.request.PortfolioRequest;
import com.casl.portfolio.utils.PortfolioUtilities;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioController.class)
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final PortfolioUtilities portfolioUtilities = new PortfolioUtilities();

    @Test
    void getPortfolioReturnsPortfolioAsJson() throws Exception {
        mockMvc.perform(get("/rest-api/v1/crud/user/{userId}", "ana")
                        .param("id", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("ana"))
                .andExpect(jsonPath("$.requestId").value(123))
                .andExpect(jsonPath("$.message").value("Portfolio fetched successfully"));
    }

    @Test
    void getPortfolioWithoutRequiredParamReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/rest-api/v1/crud/user/{userId}", "ana"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestPortfolioReturnsCreatedMessage() throws Exception {
        PortfolioRequest portfolioRequest = portfolioUtilities.createFakePortfolioRequest();

        mockMvc.perform(post("/rest-api/v1/crud/portfolio")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(portfolioRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Portfolio created"));
    }

    @Test
    void updatePortfolioReturnsUpdatedMessage() throws Exception {
        mockMvc.perform(put("/rest-api/v1/crud/user")
                        .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("Portfolio updated"));
    }

    @Test
    void deletePortfolioReturnsDeletedMessage() throws Exception {
        mockMvc.perform(delete("/rest-api/v1/crud/user"))
                .andExpect(status().isOk())
                .andExpect(content().string("Portfolio deleted"));
    }
}

