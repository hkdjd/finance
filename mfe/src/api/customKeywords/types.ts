/**
 * 自定义关键字信息
 */
export interface CustomKeyword {
  /** 关键字ID */
  id: number;
  /** 用户ID */
  userId: number;
  /** 关键字名称 */
  keyword: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/**
 * 批量创建自定义关键字请求参数
 */
export type BatchCreateKeywordsRequest = string[];

/**
 * 获取用户所有关键字响应
 */
export type GetUserKeywordsResponse = CustomKeyword[];

/**
 * 批量创建自定义关键字响应
 */
export type BatchCreateKeywordsResponse = CustomKeyword[];
