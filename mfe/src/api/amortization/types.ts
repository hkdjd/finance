// 从 contracts 模块导入共享类型,避免重复定义
// PaymentStatus 是枚举,需要作为值导入
import { PaymentStatus } from '../contracts/types';
import type { ContractBasicInfo } from '../contracts/types';

/**
 * 摊销明细条目(详细版本)
 */
export interface AmortizationEntryDetail {
  /** 条目ID(新建时为null) */
  id: number | null;
  /** 预提/摊销期间 */
  amortizationPeriod: string;
  /** 入账期间 */
  accountingPeriod: string;
  /** 摊销金额 */
  amount: number;
  /** 期间日期 */
  periodDate: string;
  /** 支付状态 */
  paymentStatus: PaymentStatus | string;
  /** 创建时间 */
  createdAt?: string;
  /** 更新时间 */
  updatedAt?: string;
  /** 创建人 */
  createdBy?: string;
  /** 更新人 */
  updatedBy?: string;
}

// 重新导出共享类型,保持向后兼容
export { PaymentStatus };
export type { ContractBasicInfo };

/**
 * 摊销明细操作请求参数
 */
export interface AmortizationOperateRequest {
  /** 合同ID */
  contractId: number;
  /** 摊销明细列表 */
  amortization: AmortizationEntryDetail[];
}

/**
 * 摊销明细操作响应
 */
export interface AmortizationOperateResponse {
  /** 合同基本信息 */
  contract: ContractBasicInfo;
  /** 摊销明细列表 */
  amortization: AmortizationEntryDetail[];
}

/**
 * 摊销明细列表查询响应
 */
export interface AmortizationListResponse {
  /** 合同基本信息 */
  contract: ContractBasicInfo;
  /** 摊销明细列表 */
  amortization: AmortizationEntryDetail[];
}
