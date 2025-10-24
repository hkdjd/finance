package com.ocbc.finance.controller;

import com.ocbc.finance.dto.JournalEntriesPreviewResponse;
import com.ocbc.finance.service.JournalService;
import com.ocbc.finance.service.calculation.AmortizationCalculationService;
import com.ocbc.finance.model.Contract;
import com.ocbc.finance.repository.ContractRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/journals")
public class JournalController {

    private final JournalService journalService;
    private final ContractRepository contractRepository;

    public JournalController(JournalService journalService,
                             AmortizationCalculationService amortizationCalculationService,
                             ContractRepository contractRepository) {
        this.journalService = journalService;
        this.contractRepository = contractRepository;
    }

    // 预览会计分录（基于步骤2返回的摊销结果）
    @PostMapping("/preview")
    public ResponseEntity<JournalEntriesPreviewResponse> preview(@RequestBody Object request,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                 LocalDate bookingDate) {
        
        // 检查请求类型
        if (request instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> requestMap = (java.util.Map<String, Object>) request;
            
            // 检查是否包含contractId字段（前端格式）
            if (requestMap.containsKey("contractId")) {
                Object contractIdObj = requestMap.get("contractId");
                Long contractId;
                
                // 处理不同类型的contractId
                if (contractIdObj instanceof Integer) {
                    contractId = ((Integer) contractIdObj).longValue();
                } else if (contractIdObj instanceof Long) {
                    contractId = (Long) contractIdObj;
                } else if (contractIdObj instanceof String) {
                    contractId = Long.parseLong((String) contractIdObj);
                } else {
                    throw new IllegalArgumentException("无效的contractId类型: " + contractIdObj.getClass());
                }
                
                String previewType = (String) requestMap.get("previewType");
                
                // 获取合同信息
                Contract contract = contractRepository.findById(contractId)
                        .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + contractId));
                
                // 根据预览类型生成符合规范的响应
                JournalEntriesPreviewResponse response = journalService.generatePreviewResponse(contract, previewType, bookingDate);
                return ResponseEntity.ok(response);
            } else {
                // 处理其他格式，返回空响应
                return ResponseEntity.ok(new JournalEntriesPreviewResponse());
            }
        } else {
            // 如果是AmortizationResponse对象（向后兼容）
            // 这种情况下我们没有合同信息，需要特殊处理
            return ResponseEntity.ok(new JournalEntriesPreviewResponse());
        }
    }
}
