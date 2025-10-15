package com.ocbc.finance.controller;

import com.ocbc.finance.dto.AmortizationListResponse;
import com.ocbc.finance.dto.AmortizationUpdateRequest;
import com.ocbc.finance.model.AmortizationEntry;
import com.ocbc.finance.service.AmortizationEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 摊销明细控制器
 * 实现步骤2的摊销明细CRUD操作
 */
@RestController
@RequestMapping("/amortization-entries")
public class AmortizationEntryController {

    private final AmortizationEntryService amortizationEntryService;

    public AmortizationEntryController(AmortizationEntryService amortizationEntryService) {
        this.amortizationEntryService = amortizationEntryService;
    }

    /**
     * 查询合同的摊销明细列表（返回包装格式：合同信息+摊销明细数组）
     */
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<AmortizationListResponse> getAmortizationEntriesByContract(@PathVariable Long contractId) {
        AmortizationListResponse response = amortizationEntryService.getAmortizationListByContract(contractId);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询单个摊销明细
     */
    @GetMapping("/{entryId}")
    public ResponseEntity<AmortizationEntry> getAmortizationEntry(@PathVariable Long entryId) {
        AmortizationEntry entry = amortizationEntryService.getAmortizationEntryById(entryId);
        return ResponseEntity.ok(entry);
    }

    /**
     * 摊销明细更新接口（只支持修改内容，不可增删行）
     * 根据需求文档步骤2：摊销明细表格不支持增删行，只支持修改
     * 请求格式与列表接口响应格式保持一致
     */
    @PostMapping("/operate")
    public ResponseEntity<AmortizationListResponse> updateAmortizationEntries(
            @Valid @RequestBody AmortizationUpdateRequest request) {
        
        try {
            // 添加请求参数验证
            if (request.getContractId() == null) {
                throw new IllegalArgumentException("合同ID不能为空");
            }
            if (request.getAmortization() == null || request.getAmortization().isEmpty()) {
                throw new IllegalArgumentException("摊销明细列表不能为空");
            }
            
            AmortizationListResponse response = amortizationEntryService.updateAmortizationEntriesFromRequest(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 记录详细错误信息
            System.err.println("摊销明细操作失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
