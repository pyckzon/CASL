package com.casl.portfolio.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RequestMapping("/rest-api/v1/crud")
@RestController
public class PortfolioController {



    @GetMapping("/user")
    public String getPortfolio() {
        log.info("INFO Fetching portfolio data");
        return "Portfolio data";
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
