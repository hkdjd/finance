/**
 * 报表相关 API
 */
import { apiGet } from '../client';
import type { DashboardReportResponse, VendorDistributionResponse } from './types';

/**
 * 获取仪表盘报表数据（柱状图）
 * 包含：生效合同数量、本月摊销金额、剩余待付款金额
 */
export const getDashboardReport = () => {
  return apiGet<DashboardReportResponse>('/reports/dashboard');
};

/**
 * 获取供应商分布报表数据（饼图）
 * 按供应商统计合同数量及占比
 */
export const getVendorDistribution = () => {
  return apiGet<VendorDistributionResponse>('/reports/vendor-distribution');
};
