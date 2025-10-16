import React, { useState, useCallback, useMemo, useEffect } from 'react';
import { Modal, Button, message, Descriptions } from 'antd';
import { EditableProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { DeleteOutlined } from '@ant-design/icons';
import { ContractConfirmModalProps, PrepaymentItem, ContractInfo } from './types';
import { updateContract, operateAmortization } from '../../api';
import type { AmortizationEntryDetail } from '../../api/amortization';
import styles from './styles.module.css';

// 时间格式化辅助函数
const formatDateTime = (dateString: string): string => {
  return new Date(dateString).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  });
};

const ContractConfirmModal: React.FC<ContractConfirmModalProps> = ({
  visible,
  contractInfo,
  prepaymentData,
  onConfirm,
  onCancel,
}) => {
  // 表格数据状态管理
  const [dataSource, setDataSource] = useState<PrepaymentItem[]>(prepaymentData);
  // 提交状态
  const [submitting, setSubmitting] = useState(false);

  // 监听弹窗显示状态和数据变化，管理数据加载和清空
  useEffect(() => {
    if (visible && prepaymentData) {
      // 弹窗打开时，强制重新加载初始数据（深拷贝确保数据独立）
      const freshData = JSON.parse(JSON.stringify(prepaymentData));
      setDataSource(freshData);
    } else if (!visible) {
      // 弹窗关闭时，清空所有数据
      setDataSource([]);
      console.log('弹窗关闭，清空数据');
    }
  }, [visible, prepaymentData]);

  // 生成新的临时ID（用于前端标识，后端会分配真实ID）
  const generateTempId = useCallback(() => {
    return `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }, []);

  // 新增行由 EditableProTable 内置的 recordCreatorProps 提供（显示在表格底部）

  // 第五步核心功能：删除行功能
  const handleDeleteRow = useCallback((record: PrepaymentItem) => {
    setDataSource(prev => prev.filter(item => item.id !== record.id));
    message.success('删除成功');
  }, []);

  // 已移除批量选择与批量删除功能

  // 表格列配置（摊销明细）
  const columns: ProColumns<PrepaymentItem>[] = useMemo(() => [
    {
      title: '预提/摊销期间',
      dataIndex: 'amortizationPeriod',
      key: 'amortizationPeriod',
      valueType: 'date',
      width: 150,
      formItemProps: {
        rules: [
          { required: true, message: '请选择预提/摊销期间' },
        ],
      },
      fieldProps: {
        format: 'YYYY-MM-DD',
        placeholder: '请选择月份',
        style: { backgroundColor: '#ffffff' },
      },
      render: (_: any, record: PrepaymentItem) => record.amortizationPeriod || '-',
    },
    {
      title: '入账期间',
      dataIndex: 'accountingPeriod',
      key: 'accountingPeriod',
      valueType: 'date',
      width: 150,
      formItemProps: {
        rules: [
          { required: true, message: '请选择入账期间' },
        ],
      },
      fieldProps: {
        format: 'YYYY-MM-DD',
        placeholder: '请选择月份',
        style: { backgroundColor: '#ffffff' },
      },
      render: (_: any, record: PrepaymentItem) => record.accountingPeriod || '-',
    },
    {
      title: '摊销金额',
      dataIndex: 'amount',
      key: 'amount',
      valueType: 'money',
      width: 150,
      formItemProps: {
        rules: [
          { required: true, message: '请输入金额' },
          { type: 'number', min: 0.01, message: '金额必须大于0' },
        ],
      },
      fieldProps: {
        precision: 2,
        min: 0,
        placeholder: '请输入金额',
        formatter: (value: string) => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ','),
        parser: (value: string) => value?.replace(/¥\s?|(,*)/g, '') || '',
      },
      render: (_: any, record: PrepaymentItem) => {
        const amount = record.amount || 0;
        return `¥${amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
      },
    },
    {
      title: '操作',
      valueType: 'option',
      key: 'option',
      width: 120,
    //   render: (_text: any, record: PrepaymentItem, _index: number, action: any) => [
    //     <Button
    //       key="edit"
    //       type="link"
    //       size="small"
    //       onClick={() => {
    //         action?.startEditable?.(record.id);
    //       }}
    //     >
    //       编辑
    //     </Button>,
    //     <Button
    //       key="delete"
    //       type="link"
    //       size="small"
    //       danger
    //       icon={<DeleteOutlined />}
    //       onClick={() => handleDeleteRow(record)}
    //     >
    //       删除
    //     </Button>,
    //   ],
    },
  ], [handleDeleteRow]);

  // 数据保存处理
  const handleSave = useCallback(async (
    key: React.Key | React.Key[], 
    record: PrepaymentItem & { index?: number },
    _originRow?: PrepaymentItem & { index?: number }
  ) => {
    const newData = [...dataSource];
    // 处理单个key的情况
    const recordKey = Array.isArray(key) ? key[0] : key;
    const index = newData.findIndex(item => recordKey === item.id);
    
    if (index > -1) {
      const item = newData[index];
      // 直接合并保存（保持数据类型一致）
      const merged: PrepaymentItem = {
        ...item,
        ...record,
        id: item.id, // 确保ID不被覆盖
      };
      newData.splice(index, 1, merged);
      setDataSource(newData);
      
      console.log('数据已保存:', merged);
      console.log('更新后的完整数据:', newData);
    }
    
    return true; // 返回成功状态
  }, [dataSource]);

  // 确认按钮处理
  // const handleConfirm = useCallback(async () => {
  //   if (!contractInfo) {
  //     message.error('合同信息不存在');
  //     return;
  //   }

  //   // 获取当前最新的表格数据
  //   const currentData = [...dataSource];
    
  //   // 验证所有行数据是否完整
  //   const invalidRows = currentData.filter(item => 
  //     !item.amortizationPeriod || !item.accountingPeriod || !item.amount || item.amount <= 0
  //   );
  //   console.log('invalidRows', invalidRows);
  //   if (invalidRows.length > 0) {
  //     message.error('请完善所有行的数据信息');
  //     return;
  //   }

  //   // 在控制台打印摊销明细表数据
  //   console.log('=== 确认时的最新摊销明细表数据 ===');
  //   console.log('数据条数:', currentData.length);
  //   console.log('详细数据:', currentData);
  //   console.table(currentData);
    
  //   setSubmitting(true);
    
  //   try {
  //     // 1. 调用更新合同信息接口
  //     const updateContractResponse = await updateContract(contractInfo.contractId, {
  //       totalAmount: contractInfo.totalAmount,
  //       startDate: contractInfo.startDate,
  //       endDate: contractInfo.endDate,
  //       taxRate: contractInfo.taxRate,
  //       vendorName: contractInfo.vendorName,
  //     });
      
  //     console.log('更新合同信息成功:', updateContractResponse);
      
  //     // 2. 调用摊销明细操作接口
  //     // 将 PrepaymentItem 转换为 AmortizationEntryDetail
  //     const amortizationEntries: AmortizationEntryDetail[] = currentData.map(item => ({
  //       // 如果 id 是临时字符串 ID（以 temp_ 开头），转换为 null（表示新增）
  //       // 如果是数字 ID，保持原值（表示更新）
  //       id: typeof item.id === 'string' && item.id.startsWith('temp_') ? null : Number(item.id),
  //       amortizationPeriod: item.amortizationPeriod,
  //       accountingPeriod: item.accountingPeriod,
  //       amount: item.amount,
  //       periodDate: item.amortizationPeriod, // 使用摊销期间作为期间日期
  //       paymentStatus: item.status || 'PENDING',
  //     }));
      
  //     const operateResponse = await operateAmortization({
  //       contractId: contractInfo.contractId,
  //       amortization: amortizationEntries,
  //     });
      
  //     console.log('摊销明细操作成功:', operateResponse);
      
  //     message.success('合同信息和摊销明细已保存');
      
  //     // 传递最新数据给父组件
  //     onConfirm(currentData);
      
  //     // 确认成功后清空数据（为下次打开做准备）
  //     setDataSource([]);
  //   } catch (error: any) {
  //     console.error('保存失败:', error);
  //     message.error(error?.message || '保存失败，请重试');
  //   } finally {
  //     setSubmitting(false);
  //   }
  // }, [dataSource, contractInfo, onConfirm]);

  // 临时的确认按钮处理（仅关闭弹窗）
  const handleConfirm = useCallback(() => {
    onCancel();
  }, [onCancel]);

  // 取消按钮处理
  const handleCancel = useCallback(() => {
    // 数据清空由 useEffect 自动处理（当 visible 变为 false 时）
    onCancel();
  }, [onCancel]);

  // 处理表格数据变化的包装函数
  const handleDataSourceChange = useCallback((value: readonly PrepaymentItem[]) => {
    console.log('handleDataSourceChange 被调用，value:', value)
    // 将只读数组转换为可变数组，实时同步最新的编辑数据
    const next = [...value] as PrepaymentItem[];
    setDataSource(next);
    
    console.log('表格数据已更新:', next);
  }, []);

  return (
    <Modal
      title="合同确认"
      open={visible}
      width={800}
      maskClosable={false}
      onCancel={handleCancel}
      footer={[
        <Button key="cancel" onClick={handleCancel} disabled={submitting}>
          取消
        </Button>,
        <Button key="confirm" type="primary" onClick={handleConfirm} loading={submitting}>
          确认
        </Button>,
      ]}
    >
      <div className={styles.modalContainer}>
        {/* 合同信息部分 */}
        {contractInfo && (
          <div className={styles.contractInfo}>
            <Descriptions
            //   title="合同信息"
              bordered
              column={2}
              size="small"
              labelStyle={{ fontWeight: 'bold', width: '120px' }}
              contentStyle={{ textAlign: 'left' }}
            >
              <Descriptions.Item label="合同附件名称" span={2}>
                {contractInfo.attachmentName}
              </Descriptions.Item>
              <Descriptions.Item label="供应商名称">
                {contractInfo.vendorName}
              </Descriptions.Item>
              <Descriptions.Item label="合同总金额">
                ¥{contractInfo.totalAmount.toFixed(2)}
              </Descriptions.Item>
              <Descriptions.Item label="合同开始时间">
                {contractInfo.startDate}
              </Descriptions.Item>
              <Descriptions.Item label="合同结束时间">
                {contractInfo.endDate}
              </Descriptions.Item>
              <Descriptions.Item label="税率">
                {(contractInfo.taxRate * 100).toFixed(2)}%
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {formatDateTime(contractInfo.createdAt)}
              </Descriptions.Item>
            </Descriptions>
          </div>
        )}

        {/* 摊销明细表部分 */}
        <div className={styles.prepaymentTable}>
          <EditableProTable<PrepaymentItem>
            rowKey="id"
            headerTitle="摊销明细表"
            columns={columns}
            value={dataSource}
            onChange={handleDataSourceChange}
            // 使用内置新增入口，显示在表格底部
            recordCreatorProps={{
              position: 'bottom',
              newRecordType: 'dataSource',
              record: () => {
                const tempId = generateTempId();
                // 获取当前日期并格式化为 YYYY-MM-DD 格式，与其他数据保持一致
                const currentDate = new Date();
                const formattedDate = currentDate.toISOString().split('T')[0]; // 格式：YYYY-MM-DD
                
                const newRecord: PrepaymentItem = {
                  id: tempId, // 使用临时ID用于前端标识
                  amortizationPeriod: formattedDate,
                  accountingPeriod: formattedDate,
                  amount: 0,
                  status: 'PENDING',
                };
                
                console.log('新增行数据:', newRecord);
                
                return newRecord;
              },
            }}
            editable={{
              type: 'multiple',
              editableKeys: dataSource
                .filter(item => item.id != null) // 过滤掉 null 和 undefined
                .map(item => String(item.id)), // 确保转换为字符串
              onSave: handleSave,
              onChange: (editableKeys) => {
                console.log('编辑状态变化:', editableKeys);
              },
              onValuesChange: (record, recordList) => {
                console.log('onValuesChange 被调用:', { record, recordList });
                // 实时更新数据源
                const updatedData = recordList as PrepaymentItem[];
                setDataSource(updatedData);
                
                console.log('数据源已更新:', updatedData);
              },
            }}
            scroll={{ x: 600 }}
            size="small"
          />
        </div>
      </div>
    </Modal>
  );
};
export default ContractConfirmModal;
