// 预提会计分录相关类型定义

// 预提分录条目
export interface PreviewEntry {
  entryType: string;           // 业务类型，如 "AMORTIZATION"
  bookingDate: string;         // 入账日期，格式：YYYY-MM-DD
  accountName: string;         // 会计科目名称
  debitAmount: number;         // 借方金额
  creditAmount: number;        // 贷方金额
  description: string;         // 分录描述
  memo: string;               // 备注
  entryOrder: number;         // 分录顺序
}

// 合同基础信息
export interface ContractInfo {
  id: number;                 // 合同ID
  totalAmount: number;        // 合同总金额
  startDate: string;          // 合同开始日期
  endDate: string;            // 合同结束日期
  vendorName: string;         // 供应商名称
}

// 预提会计分录预览请求参数
export interface JournalEntriesPreviewRequest {
  contractId: number;         // 合同ID
  previewType: string;        // 预览类型，固定值 "AMORTIZATION"
}

// 预提会计分录预览响应
export interface JournalEntriesPreviewResponse {
  contract: ContractInfo;     // 合同信息
  previewEntries: PreviewEntry[]; // 预提分录列表
}

// 日期范围筛选参数
export interface DateRangeFilter {
  startDate?: string;         // 开始日期
  endDate?: string;           // 结束日期
}

// 排序参数
export interface SortConfig {
  field: 'entryOrder' | 'bookingDate'; // 排序字段
  order: 'asc' | 'desc';      // 排序方向
}
