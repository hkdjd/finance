import { ContractUploadResponse, AmortizationEntry } from '../../api/contracts';

// 预付时间表数据项 - 扩展 AmortizationEntry 类型以支持临时字符串ID
export interface PrepaymentItem extends Omit<AmortizationEntry, 'id'> {
  id: string | number; // 移除 null，确保 id 总是有值
}

// 合同信息 - 使用 ContractUploadResponse 类型
export type ContractInfo = ContractUploadResponse;

// 组件 Props
export interface ContractConfirmModalProps {
  visible: boolean;
  contractInfo: ContractInfo | null;
  prepaymentData: PrepaymentItem[];
  onConfirm: (data: PrepaymentItem[]) => void;
  onCancel: () => void;
}

// 表格列配置类型
export interface TableColumn {
  title: string;
  dataIndex: string;
  key: string;
  editable?: boolean;
  required?: boolean;
  valueType?: 'date' | 'money' | 'text';
  width?: number;
}
