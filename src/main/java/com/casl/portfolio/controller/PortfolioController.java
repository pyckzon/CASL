package com.casl.portfolio.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RequestMapping("/rest-api/v1/crud")
@RestController
public class PortfolioController {



    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getPortfolio(@PathVariable String userId, @RequestParam Long id) {
        log.info("INFO Fetching portfolio data for user: {}, request ID: {}", userId, id);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "requestId", id,
                "message", "Portfolio fetched successfully"
        ));
    }

    @PostMapping("/user")
    public String createPortfolio() {
        return "Portfolio created";
    }
    @PutMapping("/user")
    public String updatePortfolio() {
        return "Portfolio updated";
    }
    @DeleteMapping("/user")
    public String deletePortfolio() {
        return "Portfolio deleted";
    }
}
