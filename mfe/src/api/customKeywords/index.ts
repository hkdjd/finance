import { apiGet, apiPost } from '../client';
import { 
  BatchCreateKeywordsRequest,
  GetUserKeywordsResponse,
  BatchCreateKeywordsResponse
} from './types';
import { 
  getMockUserKeywords,
  getMockBatchCreateKeywords
} from './mock';

// 是否使用 Mock 数据 - 生产环境关闭 Mock
const USE_MOCK = false;

/**
 * 获取用户的所有关键字
 * @param userId 用户ID（可选，默认为1）
 * @returns 用户关键字列表
 */
export const getUserKeywords = async (
  userId: number = 1
): Promise<GetUserKeywordsResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 300));
    return Promise.resolve(getMockUserKeywords(userId));
  }

  // 真实 API 调用
  const response = await apiGet<GetUserKeywordsResponse>(
    `/api/custom-keywords/user/${userId}`
  );
  return response as unknown as GetUserKeywordsResponse;
};

/**
 * 批量创建自定义关键字
 * @param keywords 关键字字符串数组
 * @param userId 用户ID（可选，默认为1）
 * @returns 批量创建关键字响应
 */
export const batchCreateKeywords = async (
  keywords: BatchCreateKeywordsRequest,
  userId: number = 1
): Promise<BatchCreateKeywordsResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 400));
    return Promise.resolve(getMockBatchCreateKeywords(keywords, userId));
  }

  // 真实 API 调用
  const response = await apiPost<BatchCreateKeywordsResponse>(
    `/api/custom-keywords/batch?userId=${userId}`,
    keywords
  );
  return response as unknown as BatchCreateKeywordsResponse;
};

// 导出类型
export * from './types';
