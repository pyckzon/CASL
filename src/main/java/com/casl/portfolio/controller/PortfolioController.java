package com.casl.portfolio.controller;

import com.casl.portfolio.models.request.PortfolioRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RequestMapping("/rest-api/v1/crud")
@RestController
public class PortfolioController {



    @GetMapping("/user/{userId}")//Ejemplo de como usar un @PathVariable y un @RequestParam vale la pena investigar su diferencia y cuando utilizarlas
    public ResponseEntity<Map<String, Object>> getPortfolio(@PathVariable String userId, @RequestParam Long id) {
        log.info("INFO Fetching portfolio data for user: {}, request ID: {}", userId, id);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "requestId", id,
                "message", "Portfolio fetched successfully"
        ));
    }

    @PostMapping("/portfolio")
    public String requestPortfolio(@RequestBody PortfolioRequest portfolioRequest) {
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
