package com.ocbc.finance.controller;

import com.ocbc.finance.dto.DashboardReportResponse;
import com.ocbc.finance.dto.VendorDistributionResponse;
import com.ocbc.finance.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报表控制器
 * 提供各类数据报表和统计分析接口
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 获取仪表盘报表数据（柱状图）
     * 包含：生效合同数量、本月摊销金额、剩余待付款金额
     * 
     * @return 仪表盘报表数据
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardReportResponse> getDashboardReport() {
        DashboardReportResponse report = reportService.getDashboardReport();
        return ResponseEntity.ok(report);
    }

    /**
     * 获取供应商分布报表数据（饼图）
     * 按供应商统计合同数量及占比
     * 
     * @return 供应商分布数据
     */
    @GetMapping("/vendor-distribution")
    public ResponseEntity<VendorDistributionResponse> getVendorDistribution() {
        VendorDistributionResponse report = reportService.getVendorDistribution();
        return ResponseEntity.ok(report);
    }
}
