// 预提会计分录API接口

import { apiPost } from '../client';
import { 
  JournalEntriesPreviewRequest, 
  JournalEntriesPreviewResponse 
} from './types';
import { mockJournalEntriesPreviewMap } from './mock';

// 是否使用Mock数据 - 生产环境关闭Mock
const USE_MOCK = false;

/**
 * 获取预提会计分录预览数据
 * @param params 请求参数
 * @returns Promise<JournalEntriesPreviewResponse>
 */
export const getJournalEntriesPreview = async (
  params: JournalEntriesPreviewRequest
): Promise<JournalEntriesPreviewResponse> => {
  if (USE_MOCK) {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 返回Mock数据
    const mockKey = `${params.contractId}_${params.previewType}`;
    const mockData = mockJournalEntriesPreviewMap[mockKey];
    if (!mockData) {
      throw new Error(`未找到合同ID为 ${params.contractId}，类型为 ${params.previewType} 的会计分录数据`);
    }
    
    return mockData;
  }

  // 真实API调用
  try {
    const response = await apiPost<JournalEntriesPreviewResponse>(
      '/journals/preview',
      params
    );
    return response as unknown as JournalEntriesPreviewResponse;
  } catch (error) {
    console.error('获取预提会计分录预览失败:', error);
    throw error;
  }
};

// 导出类型
export * from './types';
