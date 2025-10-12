package com.ocbc.finance.controller;

import com.ocbc.finance.dto.JournalEntryListResponse;
import com.ocbc.finance.dto.OperationRequest;
import com.ocbc.finance.model.JournalEntry;
import com.ocbc.finance.service.JournalEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 会计分录控制器
 * 实现步骤3的会计分录CRUD操作
 */
@RestController
@RequestMapping("/journal-entries")
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    public JournalEntryController(JournalEntryService journalEntryService) {
        this.journalEntryService = journalEntryService;
    }

    /**
     * 步骤3：根据合同ID生成会计分录
     */
    @PostMapping("/generate/{contractId}")
    public ResponseEntity<JournalEntryListResponse> generateJournalEntries(@PathVariable Long contractId) {
        JournalEntryListResponse response = journalEntryService.generateJournalEntriesWithResponse(contractId);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询合同的会计分录列表
     */
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<List<JournalEntry>> getJournalEntriesByContract(@PathVariable Long contractId) {
        List<JournalEntry> entries = journalEntryService.getJournalEntriesByContract(contractId);
        return ResponseEntity.ok(entries);
    }

    /**
     * 查询单个会计分录
     */
    @GetMapping("/{entryId}")
    public ResponseEntity<JournalEntry> getJournalEntry(@PathVariable Long entryId) {
        JournalEntry entry = journalEntryService.getJournalEntryById(entryId);
        return ResponseEntity.ok(entry);
    }

    /**
     * 统一操作接口（增删改）
     * 通过operate字段区分操作类型：CREATE、UPDATE、DELETE
     */
    @PostMapping("/operate")
    public ResponseEntity<?> operateJournalEntry(@Valid @RequestBody OperationRequest<JournalEntry> request) {
        switch (request.getOperate()) {
            case CREATE:
                JournalEntry createdEntry = journalEntryService.createJournalEntry(request.getData());
                return ResponseEntity.ok(createdEntry);
                
            case UPDATE:
                JournalEntry updatedEntry = journalEntryService.updateJournalEntry(request.getId(), request.getData());
                return ResponseEntity.ok(updatedEntry);
                
            case DELETE:
                journalEntryService.deleteJournalEntry(request.getId());
                return ResponseEntity.ok().build();
                
            default:
                return ResponseEntity.badRequest().body("不支持的操作类型: " + request.getOperate());
        }
    }

    /**
     * 批量操作接口
     * 支持批量增删改操作
     */
    @PostMapping("/batch-operate")
    public ResponseEntity<List<JournalEntry>> batchOperateJournalEntries(
            @Valid @RequestBody List<OperationRequest<JournalEntry>> requests) {
        List<JournalEntry> results = journalEntryService.batchOperateJournalEntries(requests);
        return ResponseEntity.ok(results);
    }
}
