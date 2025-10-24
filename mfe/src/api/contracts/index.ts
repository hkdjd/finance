import { apiGet, apiPost, apiPut } from '../client';
import { 
  ContractsListResponse, 
  PaginationParams, 
  ContractUploadResponse,
  AmortizationCalculateResponse,
  ContractAmortizationResponse,
  PaymentExecuteRequest,
  PaymentExecuteResponse,
  ContractPaymentRecordsResponse,
  UpdateContractRequest,
  UpdateContractResponse,
  AuditLogResponse
} from './types';
import { 
  mockContractsList, 
  getMockContractsListPaginated, 
  getMockContractUploadResponse,
  getMockAmortizationCalculate,
  getMockContractAmortizationEntries,
  getMockPaymentExecuteResponse,
  getMockContractPaymentRecords,
  getMockUpdateContractResponse,
  getMockAuditLogResponse
} from './mock';

// 是否使用 Mock 数据 - 生产环境关闭 Mock
const USE_MOCK = false;


/**
 * 查询所有合同列表
 * @returns 合同列表响应
 */
export const getAllContracts = async (): Promise<ContractsListResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 300));
    return Promise.resolve(mockContractsList);
  }

  // 真实 API 调用
  const response = await apiGet<ContractsListResponse>('/contracts');
  return response as unknown as ContractsListResponse;
};

/**
 * 分页查询合同列表
 * @param params 分页参数
 * @returns 合同列表响应
 */
export const getContractsList = async (
  params?: PaginationParams
): Promise<ContractsListResponse> => {
  const { page = 0, size = 10 } = params || {};

  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 300));
    return Promise.resolve(getMockContractsListPaginated(page, size));
  }

  // 真实 API 调用
  const response = await apiGet<ContractsListResponse>(
    '/contracts/list',
    { page, size }
  );
  return response as unknown as ContractsListResponse;
};

/**
 * 解析合同文件（不保存到数据库）
 * @param file 合同文件
 * @param userId 用户ID（可选），用于获取该用户的自定义关键字
 * @returns 合同解析响应（不包含contractId）
 */
export const parseContract = async (
  file: File,
  userId?: number
): Promise<ContractUploadResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟（上传较慢）
    await new Promise((resolve) => setTimeout(resolve, 800));
    const mockResponse = getMockContractUploadResponse(file.name);
    mockResponse.contractId = null; // 解析接口不返回contractId
    return Promise.resolve(mockResponse);
  }

  // 真实 API 调用
  const formData = new FormData();
  formData.append('file', file);
  if (userId !== undefined) {
    formData.append('userId', userId.toString());
  }

  const response = await apiPost<ContractUploadResponse>(
    '/contracts/parse',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response as unknown as ContractUploadResponse;
};

/**
 * 上传合同文件
 * @param file 合同文件
 * @param userId 用户ID（可选），用于获取该用户的自定义关键字
 * @returns 合同上传响应
 */
export const uploadContract = async (
  file: File,
  userId?: number
): Promise<ContractUploadResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟（上传较慢）
    await new Promise((resolve) => setTimeout(resolve, 800));
    return Promise.resolve(getMockContractUploadResponse(file.name));
  }

  // 真实 API 调用
  const formData = new FormData();
  formData.append('file', file);
  if (userId !== undefined) {
    formData.append('userId', userId.toString());
  }

  const response = await apiPost<ContractUploadResponse>(
    '/contracts/upload',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response as unknown as ContractUploadResponse;
};

/**
 * 创建合同并初始化摊销明细
 * @param request 创建合同请求参数
 * @returns 合同创建响应（包含合同信息和摊销明细）
 */
export const createContract = async (
  request: {
    totalAmount: number;
    startDate: string;
    endDate: string;
    vendorName: string;
    taxRate: number;
    operatorId?: string;
  }
): Promise<AmortizationCalculateResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 500));
    return Promise.resolve({
      contractId: Date.now(),
      totalAmount: request.totalAmount,
      startDate: request.startDate,
      endDate: request.endDate,
      vendorName: request.vendorName,
      taxRate: request.taxRate,
      entries: []
    });
  }

  // 真实 API 调用
  const response = await apiPost<AmortizationCalculateResponse>(
    '/contracts',
    request
  );
  return response as unknown as AmortizationCalculateResponse;
};

/**
 * 计算合同摊销明细
 * @param contractId 合同ID
 * @returns 摊销计算响应
 */
export const calculateAmortization = async (
  contractId: number
): Promise<AmortizationCalculateResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 400));
    return Promise.resolve(getMockAmortizationCalculate(contractId));
  }

  // 真实 API 调用
  const response = await apiGet<AmortizationCalculateResponse>(
    `/amortization/calculate/${contractId}`
  );
  return response as unknown as AmortizationCalculateResponse;
};

/**
 * 查询合同摊销明细列表
 * @param contractId 合同ID
 * @returns 合同摊销明细响应
 */
export const getContractAmortizationEntries = async (
  contractId: number
): Promise<ContractAmortizationResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 300));
    return Promise.resolve(getMockContractAmortizationEntries(contractId));
  }

  // 真实 API 调用 (待替换为真实 URL)
  const response = await apiGet<ContractAmortizationResponse>(
    `/amortization-entries/contract/${contractId}`
  );
  return response as unknown as ContractAmortizationResponse;
};

/**
 * 执行支付
 * @param request 支付请求参数
 * @returns 支付执行响应
 */
export const executePayment = async (
  request: PaymentExecuteRequest
): Promise<PaymentExecuteResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 500));
    return Promise.resolve(getMockPaymentExecuteResponse(request));
  }

  // 真实 API 调用 (待替换为真实 URL)
  const response = await apiPost<PaymentExecuteResponse>(
    '/payments/execute',
    request
  );
  return response as unknown as PaymentExecuteResponse;
};

/**
 * 查询合同支付记录列表（预提会计分录）
 * @param contractId 合同ID
 * @returns 合同支付记录列表
 */
export const getContractPaymentRecords = async (
  contractId: number
): Promise<ContractPaymentRecordsResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 300));
    return Promise.resolve(getMockContractPaymentRecords(contractId));
  }

  // 真实 API 调用 (待替换为真实 URL)
  const response = await apiGet<ContractPaymentRecordsResponse>(
    `/payments/contracts/${contractId}`
  );
  return response as unknown as ContractPaymentRecordsResponse;
}
/*
 * 更新合同信息
 * @param contractId 合同ID
 * @param request 更新合同请求参数
 * @returns 更新合同响应
 */
export const updateContract = async (
  contractId: number,
  request: UpdateContractRequest
): Promise<UpdateContractResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 400));
    return Promise.resolve(getMockUpdateContractResponse(contractId, request));
  }

  // 真实 API 调用
  const response = await apiPut<UpdateContractResponse>(
    `/contracts/${contractId}`,
    request
  );
  return response as unknown as UpdateContractResponse;
};

/**
 * 获取摊销明细的审计日志
 * @param amortizationEntryId 摊销明细ID
 * @returns 审计日志响应
 */
export const getAuditLogsByAmortizationEntryId = async (
  amortizationEntryId: number
): Promise<AuditLogResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 300));
    return Promise.resolve(getMockAuditLogResponse(amortizationEntryId));
  }

  // 真实 API 调用
  const response = await apiGet<AuditLogResponse>(
    `/audit-logs/amortization-entry/${amortizationEntryId}`
  );
  return response as unknown as AuditLogResponse;
};

/**
 * 更新合同状态
 * @param contractId 合同ID
 * @param status 新的合同状态
 * @returns 更新结果
 */
export const updateContractStatus = async (
  contractId: number,
  status: string
): Promise<{ message: string; status: string }> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 300));
    return Promise.resolve({
      message: `合同状态已更新为${status === 'COMPLETED' ? '已完成' : status}`,
      status
    });
  }

  // 真实 API 调用
  const response = await apiPut<{ message: string; status: string }>(
    `/contracts/${contractId}/status`,
    { status }
  );
  return response as unknown as { message: string; status: string };
};

/**
 * 创建操作记录
 * @param operationLog 操作记录数据
 * @returns 创建结果
 */
export const createOperationLog = async (operationLog: {
  contractId: number;
  operationType: string;
  description: string;
  operator: string;
  operationTime?: string;
}): Promise<{ success: boolean; message: string; data: any }> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 200));
    return Promise.resolve({
      success: true,
      message: '操作记录创建成功',
      data: {
        id: Date.now(),
        ...operationLog,
        operationTime: operationLog.operationTime || new Date().toISOString(),
        createdAt: new Date().toISOString()
      }
    });
  }

  // 真实 API 调用
  const response = await apiPost<{ success: boolean; message: string; data: any }>(
    '/api/operation-logs',
    operationLog
  );
  return response as unknown as { success: boolean; message: string; data: any };
};

/**
 * 获取合同的操作记录列表
 * @param contractId 合同ID
 * @returns 操作记录列表
 */
export const getOperationLogsByContractId = async (
  contractId: number
): Promise<{ success: boolean; message: string; data: any[]; total: number }> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 200));
    
    // 从localStorage获取操作记录
    const storageKey = `operationLogs_${contractId}`;
    const storedLogs = localStorage.getItem(storageKey);
    const logs = storedLogs ? JSON.parse(storedLogs) : [];
    
    return Promise.resolve({
      success: true,
      message: '查询成功',
      data: logs,
      total: logs.length
    });
  }

  // 真实 API 调用
  const response = await apiGet<{ success: boolean; message: string; data: any[]; total: number }>(
    `/api/operation-logs/contract/${contractId}`
  );
  return response as unknown as { success: boolean; message: string; data: any[]; total: number };
};

// 导出类型
export * from './types';
