import { apiGet, apiPost } from '../client';
import {
  AmortizationListResponse,
  AmortizationOperateRequest,
  AmortizationOperateResponse,
} from './types';
import {
  getMockAmortizationList,
  getMockAmortizationOperateResponse,
} from './mock';

// 是否使用 Mock 数据 - 生产环境关闭Mock
const USE_MOCK = false;


/**
 * 查询指定合同的摊销明细列表
 * @param contractId 合同ID
 * @returns 摊销明细列表响应
 */
export const getAmortizationList = async (
  contractId: number
): Promise<AmortizationListResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 300));
    return Promise.resolve(getMockAmortizationList(contractId));
  }

  // 真实 API 调用
  const response = await apiGet<AmortizationListResponse>(
    `/amortization-entries/contract/${contractId}`
  );
  return response as unknown as AmortizationListResponse;
};

/**
 * 摊销明细操作接口（支持增删改）
 * @param request 操作请求参数
 * @returns 操作响应
 * 
 * @description
 * 操作逻辑：
 * - 新增：当摊销明细的 id 为 null 时，创建新的摊销明细
 * - 更新：当摊销明细的 id 与数据库中的ID一致时，更新该摊销明细
 * - 删除：当数据库中存在的摊销明细ID在请求中不存在时，删除该摊销明细
 */
export const operateAmortization = async (
  request: AmortizationOperateRequest
): Promise<AmortizationOperateResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 500));
    return Promise.resolve(getMockAmortizationOperateResponse(request));
  }

  // 真实 API 调用
  const response = await apiPost<AmortizationOperateResponse>(
    '/amortization-entries/operate',
    request
  );
  return response as unknown as AmortizationOperateResponse;
};

// 导出类型
export * from './types';
