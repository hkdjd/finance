/**
 * 报表相关类型定义
 */

/**
 * 仪表盘报表响应（柱状图数据）
 */
export interface DashboardReportResponse {
  /** 生效合同数量 */
  activeContractCount: number;
  /** 本月摊销金额 */
  currentMonthAmortization: number;
  /** 剩余待付款金额 */
  remainingPayableAmount: number;
  /** 统计月份（格式：yyyy-MM） */
  statisticsMonth: string;
  /** 数据生成时间 */
  generatedAt: string;
}

/**
 * 供应商分布项
 */
export interface VendorDistributionItem {
  /** 供应商名称 */
  vendorName: string;
  /** 合同数量 */
  contractCount: number;
  /** 占比百分比（保留2位小数） */
  percentage: number;
}

/**
 * 供应商分布报表响应（饼图数据）
 */
export interface VendorDistributionResponse {
  /** 供应商分布数据列表 */
  vendors: VendorDistributionItem[];
  /** 合同总数 */
  totalContracts: number;
  /** 数据生成时间 */
  generatedAt: string;
}
