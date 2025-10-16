/**
 * 合同状态枚举
 */
export enum ContractStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  EXPIRED = 'EXPIRED',
}

/**
 * 合同信息
 */
export interface Contract {
  /** 合同ID */
  contractId: number;
  /** 合同总金额 */
  totalAmount: number;
  /** 合同开始时间 */
  startDate: string;
  /** 合同结束时间 */
  endDate: string;
  /** 供应商名称 */
  vendorName: string;
  /** 合同附件名称 */
  attachmentName: string;
  /** 创建时间 */
  createdAt: string;
  /** 合同状态 */
  status: ContractStatus | string;
}

/**
 * 合同列表响应
 */
export interface ContractsListResponse {
  /** 合同列表数组 */
  contracts: Contract[];
  /** 总数量 */
  totalCount: number;
  /** 操作消息 */
  message: string;
}

/**
 * 分页查询参数
 */
export interface PaginationParams {
  /** 页码，从0开始，默认0 */
  page?: number;
  /** 每页大小，默认10 */
  size?: number;
}

/**
 * 合同上传响应
 */
export interface ContractUploadResponse {
  /** 合同ID */
  contractId: number;
  /** 合同总金额 */
  totalAmount: number;
  /** 合同开始时间 */
  startDate: string;
  /** 合同结束时间 */
  endDate: string;
  /** 税率 */
  taxRate: number;
  /** 供应商名称 */
  vendorName: string;
  /** 合同附件名称 */
  attachmentName: string;
  /** 创建时间 */
  createdAt: string;
  /** 操作消息 */
  message: string;
}

/**
 * 摊销场景枚举
 */
export enum AmortizationScenario {
  /** 场景1：当前时间小于合同开始时间 */
  SCENARIO_1 = 'SCENARIO_1',
  /** 场景2：当前时间在合同开始时间内 */
  SCENARIO_2 = 'SCENARIO_2',
  /** 场景3：当前时间大于合同结束时间 */
  SCENARIO_3 = 'SCENARIO_3',
}

/**
 * 摊销条目状态枚举
 */
export enum AmortizationStatus {
  /** 待处理 */
  PENDING = 'PENDING',
  /** 已完成 */
  COMPLETED = 'COMPLETED',
}

/**
 * 摊销明细条目
 */
export interface AmortizationEntry {
  /** 条目ID（新建时为null） */
  id: number | null;
  /** 预提/摊销期间 */
  amortizationPeriod: string;
  /** 入账期间 */
  accountingPeriod: string;
  /** 摊销金额 */
  amount: number;
  /** 状态 */
  status?: AmortizationStatus | string;
}

/**
 * 摊销计算响应
 */
export interface AmortizationCalculateResponse {
  /** 合同总金额 */
  totalAmount: number;
  /** 开始日期（YYYY-MM格式） */
  startDate: string;
  /** 结束日期（YYYY-MM格式） */
  endDate: string;
  /** 摊销场景 */
  scenario: AmortizationScenario | string;
  /** 生成时间 */
  generatedAt: string;
  /** 摊销明细条目列表 */
  entries: AmortizationEntry[];
}

/**
 * 付款状态枚举
 */
export enum PaymentStatus {
  /** 待付款 */
  PENDING = 'PENDING',
  /** 已付款 */
  PAID = 'PAID',
  /** 逾期 */
  OVERDUE = 'OVERDUE',
}

/**
 * 合同摊销明细条目（详细版本）
 */
export interface ContractAmortizationEntry {
  /** 条目ID */
  id: number;
  /** 预提期间 */
  amortizationPeriod: string;
  /** 会计期间 */
  accountingPeriod: string;
  /** 金额 */
  amount: number;
  /** 期间日期 */
  periodDate: string;
  /** 付款状态 */
  paymentStatus: PaymentStatus | string;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
  /** 创建人 */
  createdBy: string;
  /** 更新人 */
  updatedBy: string;
}

/**
 * 合同基本信息（用于摊销明细查询）
 */
export interface ContractBasicInfo {
  /** 合同ID */
  id: number;
  /** 合同总金额 */
  totalAmount: number;
  /** 合同开始日期 */
  startDate: string;
  /** 合同结束日期 */
  endDate: string;
  /** 供应商名称 */
  vendorName: string;
}

/**
 * 合同摊销明细查询响应
 */
export interface ContractAmortizationResponse {
  /** 合同基本信息 */
  contract: ContractBasicInfo;
  /** 摊销明细列表 */
  amortization: ContractAmortizationEntry[];
}

/**
 * 支付请求参数
 */
export interface PaymentExecuteRequest {
  /** 合同ID */
  contractId: number;
  /** 支付金额 */
  paymentAmount: number;
  /** 记账日期 */
  bookingDate: string;
  /** 选中的账期 */
  selectedPeriods: number[];
}

/**
 * 会计分录条目
 */
export interface JournalEntry {
  /** 记账日期 */
  bookingDate: string;
  /** 科目 */
  account: string;
  /** 借方金额 */
  dr: number;
  /** 贷方金额 */
  cr: number;
  /** 备注 */
  memo: string;
}

/**
 * 支付执行响应
 */
export interface PaymentExecuteResponse {
  /** 支付ID */
  paymentId: number;
  /** 合同ID */
  contractId: number;
  /** 支付金额 */
  paymentAmount: number;
  /** 记账日期 */
  bookingDate: string;
  /** 选中的账期 */
  selectedPeriods: string[];
  /** 支付状态 */
  status: string;
  /** 会计分录 */
  journalEntries: JournalEntry[];
  /** 操作消息 */
  message: string;
}

/**
 * 支付记录（预提会计分录）
 */
export interface PaymentRecord {
  /** 支付ID */
  paymentId: number;
  /** 合同ID */
  contractId: number;
  /** 支付金额 */
  paymentAmount: number;
  /** 记账日期 */
  bookingDate: string;
  /** 选中的账期 */
  selectedPeriods: string[];
  /** 支付状态 */
  status: string;
  /** 会计分录 */
  journalEntries: JournalEntry[];
}

/**
 * 合同支付记录列表响应
 */
export type ContractPaymentRecordsResponse = PaymentRecord[];
/* 
* 更新合同请求参数
 */
export interface UpdateContractRequest {
  /** 合同总金额，必须大于0 */
  totalAmount: number;
  /** 合同开始时间，格式：yyyy-MM-dd */
  startDate: string;
  /** 合同结束时间，格式：yyyy-MM-dd */
  endDate: string;
  /** 税率，如0.06表示6% */
  taxRate: number;
  /** 供应商名称，不能为空 */
  vendorName: string;
}

/**
 * 更新合同响应
 */
export interface UpdateContractResponse {
  /** 合同ID */
  contractId: number;
  /** 合同总金额 */
  totalAmount: number;
  /** 合同开始时间 */
  startDate: string;
  /** 合同结束时间 */
  endDate: string;
  /** 税率 */
  taxRate: number;
  /** 供应商名称 */
  vendorName: string;
  /** 合同附件名称 */
  attachmentName: string;
  /** 创建时间 */
  createdAt: string;
  /** 操作消息 */
  message: string;
}
