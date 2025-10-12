package com.ocbc.finance.controller;

import com.ocbc.finance.dto.AmortizationResponse;
import com.ocbc.finance.dto.JournalEntryDto;
import com.ocbc.finance.service.JournalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/journals")
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    // 预览会计分录（基于步骤2返回的摊销结果）
    @PostMapping("/preview")
    public ResponseEntity<List<JournalEntryDto>> preview(@RequestBody AmortizationResponse amort,
                                                         @RequestParam(required = false)
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                         LocalDate bookingDate) {
        List<JournalEntryDto> list = journalService.previewFromAmortization(amort, bookingDate);
        return ResponseEntity.ok(list);
    }
}
