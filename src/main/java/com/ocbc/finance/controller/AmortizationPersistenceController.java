package com.ocbc.finance.controller;

import com.ocbc.finance.dto.AmortizationResponse;
import com.ocbc.finance.service.ContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/amortization")
public class AmortizationPersistenceController {

    private final ContractService contractApplicationService;

    public AmortizationPersistenceController(ContractService contractApplicationService) {
        this.contractApplicationService = contractApplicationService;
    }

    /** 更新摊销明细金额 */
    @PutMapping("/entries/{entryId}")
    public ResponseEntity<AmortizationResponse> updateAmortizationEntry(
            @PathVariable Long entryId,
            @RequestParam BigDecimal amount) {
        AmortizationResponse response = contractApplicationService.updateAmortizationEntry(entryId, amount);
        return ResponseEntity.ok(response);
    }
}
