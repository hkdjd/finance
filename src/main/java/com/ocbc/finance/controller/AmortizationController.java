package com.ocbc.finance.controller;

import com.ocbc.finance.dto.AmortizationResponse;
import com.ocbc.finance.service.calculation.AmortizationCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/amortization")
public class AmortizationController {

    private final AmortizationCalculationService calculationService;

    public AmortizationController(AmortizationCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @GetMapping("/calculate/{contractId}")
    public ResponseEntity<AmortizationResponse> calculate(@PathVariable Long contractId) {
        AmortizationResponse resp = calculationService.calculateByContractId(contractId);
        return ResponseEntity.ok(resp);
    }
}
