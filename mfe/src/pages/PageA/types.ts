// 文件上传相关类型
export interface UploadFile {
  uid: string;
  name: string;
  status: 'uploading' | 'done' | 'error';
  url?: string;
  size?: number;
  type?: string;
}

// 合同数据类型 - 使用 API 定义的类型
import { Contract } from '../../api/contracts';
export type ContractData = Contract;

// 页面状态类型
export interface PageAState {
  uploadedFiles: UploadFile[];
  contractList: Contract[];
  loading: boolean;
  error: string | null;
}

// PageA 组件 Props
export interface PageAProps {}

// 上传组件配置类型
export interface UploadConfig {
  name: string;
  multiple: boolean;
  action: string;
  accept?: string;
  maxCount?: number;
}
