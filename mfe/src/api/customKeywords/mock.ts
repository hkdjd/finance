import { 
  CustomKeyword,
  GetUserKeywordsResponse,
  BatchCreateKeywordsResponse
} from './types';

/**
 * Mock 用户关键字列表数据
 */
export const mockUserKeywords: GetUserKeywordsResponse = [
  {
    id: 1,
    userId: 1,
    keyword: '法人',
    createTime: '2025-10-21T21:45:07.321016',
    updateTime: '2025-10-21T21:45:07.321016'
  },
  {
    id: 2,
    userId: 1,
    keyword: '项目名称',
    createTime: '2025-10-21T21:46:15.123456',
    updateTime: '2025-10-21T21:46:15.123456'
  },
  {
    id: 3,
    userId: 1,
    keyword: '合同编号',
    createTime: '2025-10-21T21:47:20.654321',
    updateTime: '2025-10-21T21:47:20.654321'
  }
];

/**
 * 生成获取用户关键字 Mock 数据
 * @param userId 用户ID
 */
export const getMockUserKeywords = (userId: number = 1): GetUserKeywordsResponse => {
  // 模拟根据用户ID返回不同的关键字
  return mockUserKeywords.map(keyword => ({
    ...keyword,
    userId
  }));
};

/**
 * 生成批量创建关键字 Mock 数据
 * @param keywords 关键字数组
 * @param userId 用户ID
 */
export const getMockBatchCreateKeywords = (
  keywords: string[],
  userId: number = 1
): BatchCreateKeywordsResponse => {
  const timestamp = new Date().toISOString();
  let startId = Math.floor(Math.random() * 1000) + 1;
  
  return keywords.map(keyword => ({
    id: startId++,
    userId,
    keyword,
    createTime: timestamp,
    updateTime: timestamp
  }));
};
