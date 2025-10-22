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
  UpdateContractResponse
} from './types';
import { 
  mockContractsList, 
  getMockContractsListPaginated, 
  getMockContractUploadResponse,
  getMockAmortizationCalculate,
  getMockContractAmortizationEntries,
  getMockPaymentExecuteResponse,
  getMockContractPaymentRecords,
  getMockUpdateContractResponse
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
 * 上传合同文件
 * @param file 合同文件
 * @returns 合同上传响应
 */
export const uploadContract = async (file: File): Promise<ContractUploadResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟（上传较慢）
    await new Promise((resolve) => setTimeout(resolve, 800));
    return Promise.resolve(getMockContractUploadResponse(file.name));
  }

  // 真实 API 调用
  const formData = new FormData();
  formData.append('file', file);

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

// 导出类型
export * from './types';
