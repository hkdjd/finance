// package com.ocbc.finance.controller;

// import com.ocbc.finance.dto.*;
// import com.ocbc.finance.model.Contract;
// import com.ocbc.finance.model.JournalEntry;
// import com.ocbc.finance.service.MockTestService;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.math.BigDecimal;
// import java.util.List;

// /**
//  * Mock测试控制器
//  * 用于测试三组合同的摊销和付款会计分录生成
//  */
// @RestController
// @RequestMapping("/api/mock-test")
// @CrossOrigin(origins = "*")
// public class MockTestController {

//     private final MockTestService mockTestService;

//     public MockTestController(MockTestService mockTestService) {
//         this.mockTestService = mockTestService;
//     }

//     /**
//      * 获取所有Mock合同信息
//      */
//     @GetMapping("/contracts")
//     public ResponseEntity<List<Contract>> getAllMockContracts() {
//         List<Contract> contracts = mockTestService.getAllMockContracts();
//         return ResponseEntity.ok(contracts);
//     }

//     /**
//      * 为指定合同生成摊销会计分录
//      */
//     @PostMapping("/contracts/{contractId}/amortization-entries")
//     public ResponseEntity<List<JournalEntry>> generateAmortizationEntries(@PathVariable Long contractId) {
//         try {
//             List<JournalEntry> entries = mockTestService.generateAmortizationEntries(contractId);
//             return ResponseEntity.ok(entries);
//         } catch (Exception e) {
//             return ResponseEntity.badRequest().build();
//         }
//     }

//     /**
//      * 执行第一个月付款
//      */
//     @PostMapping("/contracts/{contractId}/payment/first-month")
//     public ResponseEntity<PaymentExecutionResponse> payFirstMonth(@PathVariable Long contractId) {
//         try {
//             PaymentExecutionResponse response = mockTestService.executeFirstMonthPayment(contractId);
//             return ResponseEntity.ok(response);
//         } catch (Exception e) {
//             return ResponseEntity.badRequest().build();
//         }
//     }

//     /**
//      * 超额付款测试
//      */
//     @PostMapping("/contracts/{contractId}/payment/overpay")
//     public ResponseEntity<PaymentPreviewResponse> testOverpayment(@PathVariable Long contractId,
//                                                                 @RequestParam BigDecimal amount) {
//         try {
//             PaymentPreviewResponse response = mockTestService.testOverpayment(contractId, amount);
//             return ResponseEntity.ok(response);
//         } catch (Exception e) {
//             return ResponseEntity.badRequest().build();
//         }
//     }

//     /**
//      * 跨期付款测试
//      */
//     @PostMapping("/contracts/{contractId}/payment/cross-period")
//     public ResponseEntity<PaymentPreviewResponse> testCrossPeriodPayment(@PathVariable Long contractId,
//                                                                        @RequestParam BigDecimal amount,
//                                                                        @RequestParam int monthCount) {
//         try {
//             PaymentPreviewResponse response = mockTestService.testCrossPeriodPayment(contractId, amount, monthCount);
//             return ResponseEntity.ok(response);
//         } catch (Exception e) {
//             return ResponseEntity.badRequest().build();
//         }
//     }

//     /**
//      * 清理重复数据并重新初始化
//      */
//     @PostMapping("/reset-data")
//     public ResponseEntity<String> resetMockData() {
//         try {
//             mockTestService.resetMockData();
//             return ResponseEntity.ok("数据重置成功，已重新初始化Mock数据");
//         } catch (Exception e) {
//             return ResponseEntity.badRequest().body("数据重置失败: " + e.getMessage());
//         }
//     }
// }
