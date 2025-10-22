import React, { useState, useCallback, useMemo, useEffect, useRef } from 'react';
import { Button, message, Descriptions, Card, Modal, Input, InputNumber, DatePicker, Skeleton } from 'antd';
import dayjs from 'dayjs';
import { useNavigate, useLocation } from 'react-router-dom';
import { useNavigationGuard } from '../../contexts/NavigationGuardContext';
import { EditableProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Document, Page, pdfjs } from 'react-pdf';
import { PrepaymentItem } from './types';
import styles from './styles.module.css';
import { updateContract } from '../../api/contracts';
import { operateAmortization } from '../../api/amortization';
import pdfFile from '../../constants/contract_20251015_200057_504d8439.pdf';

// 配置 PDF.js worker
pdfjs.GlobalWorkerOptions.workerSrc = `https://unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;
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

const ContractPreview: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { setGuard } = useNavigationGuard();
  
  // 从路由状态中获取合同信息和摘销数据
  const { contractInfo, prepaymentData } = location.state || {};
  
  // 获取 PDF 预览地址（优先使用 attachmentPath）
  const pdfUrl = contractInfo?.attachmentPath || '';
  
  // 表格数据状态管理
  const [dataSource, setDataSource] = useState<PrepaymentItem[]>(prepaymentData || []);
  // 提交状态
  const [submitting, setSubmitting] = useState(false);
  // 合同信息可编辑状态
  const [editableContractInfo, setEditableContractInfo] = useState(contractInfo || {});
  // 校验错误状态
  const [validationErrors, setValidationErrors] = useState<{
    attachmentName?: boolean;
    vendorName?: boolean;
    totalAmount?: boolean;
    startDate?: boolean;
    endDate?: boolean;
    taxRate?: boolean;
  }>({});
  // 摘销明细表错误行索引
  const [tableErrorRows, setTableErrorRows] = useState<Set<number>>(new Set());
  // PDF 相关状态
  const [pdfLoading, setPdfLoading] = useState<boolean>(true);
  const [numPages, setNumPages] = useState<number>(0);
  const [pdfWidth, setPdfWidth] = useState<number>(600);
  // 分栏宽度比例状态
  const [leftWidth, setLeftWidth] = useState<number>(50); // 百分比
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const leftPanelRef = useRef<HTMLDivElement>(null);

  // 监听数据变化
  useEffect(() => {
    if (prepaymentData) {
      const freshData = JSON.parse(JSON.stringify(prepaymentData));
      setDataSource(freshData);
    }
  }, [prepaymentData]);

  // 如果没有合同信息，重定向回列表页
  useEffect(() => {
    if (!contractInfo) {
      message.warning('未找到合同信息');
      navigate('/page-a');
    }
  }, [contractInfo, navigate]);

  // 生成新的临时ID（用于前端标识，后端会分配真实ID）
  const generateTempId = useCallback(() => {
    return `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }, []);

  // 删除行功能
  const handleDeleteRow = useCallback((record: PrepaymentItem) => {
    setDataSource(prev => prev.filter(item => item.id !== record.id));
    message.success('删除成功');
  }, []);

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
        inputReadOnly: true, // 禁止手动输入
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
        picker: 'month',
        format: 'YYYY-MM',
        placeholder: '请选择月份',
        style: { backgroundColor: '#ffffff' },
        inputReadOnly: true, // 禁止手动输入
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
      fieldProps: (_form, { rowIndex }) => ({
        precision: 2,
        min: 0,
        placeholder: '请输入金额',
        parser: (value: string) => value?.replace(/¥\s?|(,*)/g, '') || '',
        status: tableErrorRows.has(rowIndex) ? 'error' : '',
      }),
      render: (_: any, record: PrepaymentItem) => {
        const amount = record.amount || 0;
        return `¥${amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
      },
    },
    {
      title: '操作',
      valueType: 'option',
      key: 'option',
      width: 80,
      render: (_, record) => (
        <a
          key="delete"
          onClick={() => handleDeleteRow(record)}
          style={{ color: '#ff4d4f' }}
        >
          删除
        </a>
      ),
    },
  ], [handleDeleteRow, tableErrorRows]);

  // 数据保存处理
  const handleSave = useCallback(async (
    key: React.Key | React.Key[], 
    record: PrepaymentItem & { index?: number },
    _originRow?: PrepaymentItem & { index?: number }
  ) => {
    const newData = [...dataSource];
    const recordKey = Array.isArray(key) ? key[0] : key;
    const index = newData.findIndex(item => recordKey === item.id);
    
    if (index > -1) {
      const item = newData[index];
      const merged: PrepaymentItem = {
        ...item,
        ...record,
        id: item.id,
      };
      newData.splice(index, 1, merged);
      setDataSource(newData);
      
      console.log('数据已保存:', merged);
      console.log('更新后的完整数据:', newData);
    }
    
    return true;
  }, [dataSource]);

  // 设置导航守卫
  useEffect(() => {
    setGuard({
      shouldBlock: true,
      message: '确定放弃本次合同分析？',
      onConfirm: () => {
        // 确认后的回调
      },
    });

    // 组件卸载时移除守卫
    return () => {
      setGuard(null);
    };
  }, [setGuard]);

  // 取消按钮处理 - 显示确认对话框
  const handleCancel = useCallback(() => {
    Modal.confirm({
      title: '确认操作',
      content: '确定放弃本次合同分析？',
      okText: '确认',
      cancelText: '取消',
      onOk: () => {
        setGuard(null); // 移除守卫
        navigate('/page-a');
      },
    });
  }, [navigate, setGuard]);

  // 确认按钮处理 - 校验并打印数据
  const handleConfirm = useCallback(() => {
    // 清空之前的错误状态
    const errors: typeof validationErrors = {};
    let hasError = false;

    // 1. 校验合同信息 - 所有字段不能为空
    if (!editableContractInfo.attachmentName || editableContractInfo.attachmentName.trim() === '') {
      errors.attachmentName = true;
      hasError = true;
    }
    if (!editableContractInfo.vendorName || editableContractInfo.vendorName.trim() === '') {
      errors.vendorName = true;
      hasError = true;
    }
    if (!editableContractInfo.totalAmount || editableContractInfo.totalAmount <= 0) {
      errors.totalAmount = true;
      hasError = true;
    }
    if (!editableContractInfo.startDate || editableContractInfo.startDate.trim() === '') {
      errors.startDate = true;
      hasError = true;
    }
    if (!editableContractInfo.endDate || editableContractInfo.endDate.trim() === '') {
      errors.endDate = true;
      hasError = true;
    }
    if (editableContractInfo.taxRate === undefined || editableContractInfo.taxRate === null || editableContractInfo.taxRate < 0) {
      errors.taxRate = true;
      hasError = true;
    }

    // 2. 校验合同结束时间不能小于开始时间
    if (editableContractInfo.startDate && editableContractInfo.endDate) {
      const startDate = dayjs(editableContractInfo.startDate);
      const endDate = dayjs(editableContractInfo.endDate);
      if (endDate.isBefore(startDate)) {
        errors.endDate = true;
        hasError = true;
        message.error('合同结束时间不能小于开始时间！');
      }
    }

    // 设置错误状态
    setValidationErrors(errors);

    if (hasError) {
      message.error('请填写完整的合同信息！');
      return;
    }

    // 3. 校验摘销明细表
    if (dataSource.length === 0) {
      message.error('摘销明细表不能为空，请至少添加一条记录！');
      return;
    }

    // 校验每一条记录的字段
    const invalidRecords: string[] = [];
    const errorRowIndexes = new Set<number>();
    
    dataSource.forEach((item, index) => {
      const errors: string[] = [];
      
      if (!item.amortizationPeriod || item.amortizationPeriod.trim() === '') {
        errors.push('预提/摘销期间');
      }
      if (!item.accountingPeriod || item.accountingPeriod.trim() === '') {
        errors.push('入账期间');
      }
      if (!item.amount || item.amount <= 0) {
        errors.push('摘销金额');
      }
      
      if (errors.length > 0) {
        invalidRecords.push(`第${index + 1}行: ${errors.join('、')}`);
        errorRowIndexes.add(index);
      }
    });

    // 设置错误行索引
    setTableErrorRows(errorRowIndexes);

    if (invalidRecords.length > 0) {
      message.error({
        content: (
          <div>
            <div>摘销明细表存在以下错误：</div>
            {invalidRecords.map((err, idx) => (
              <div key={idx} style={{ marginTop: 4 }}>{err}</div>
            ))}
          </div>
        ),
        duration: 5,
      });
      return;
    }

    // 清除错误状态
    setTableErrorRows(new Set());

    // 打印修改后的合同信息和摘销明细表数据
    console.log('=== 修改后的合同信息 ===');
    console.log(editableContractInfo);
    console.log('=== 摘销明细表数据 ===');
    console.log(dataSource);

    // 调用接口提交数据
    handleSubmitData();
  }, [setGuard, dataSource, editableContractInfo]);

  // 提交数据到后端
  const handleSubmitData = useCallback(async () => {
    setSubmitting(true);
    try {
      // 1. 调用 updateContract 接口更新合同信息
      message.loading({ content: '正在更新合同信息...', key: 'update' });
      const updateContractRequest = {
        totalAmount: editableContractInfo.totalAmount,
        startDate: editableContractInfo.startDate,
        endDate: editableContractInfo.endDate,
        taxRate: editableContractInfo.taxRate,
        vendorName: editableContractInfo.vendorName,
        customFields: editableContractInfo.customFields
      };
      await updateContract(editableContractInfo.contractId, updateContractRequest);
      message.success({ content: '合同信息更新成功', key: 'update' });

      // 2. 调用 operateAmortization 接口更新摘销明细
      message.loading({ content: '正在更新摘销明细...', key: 'amortization' });
      const operateAmortizationRequest = {
        contractId: editableContractInfo.contractId,
        amortization: dataSource.map(item => ({
          id: typeof item.id === 'string' ? null : item.id, // 字符串ID表示新建，转为null
          amortizationPeriod: item.amortizationPeriod,
          accountingPeriod: item.accountingPeriod,
          amount: item.amount,
          periodDate: item.amortizationPeriod, // 使用摊销期间作为期间日期
          paymentStatus: 'PENDING' // 默认状态为待处理
        }))
      };
      await operateAmortization(operateAmortizationRequest);
      message.success({ content: '摘销明细更新成功', key: 'amortization' });

      // 3. 两个接口都成功后，移除守卫并跳转到合同详情页
      setGuard(null);
      message.success('合同信息提交成功！');
      navigate(`/contract/${editableContractInfo.contractId}`);
    } catch (error) {
      console.error('提交失败:', error);
      message.error('提交失败，请重试');
    } finally {
      setSubmitting(false);
    }
  }, [editableContractInfo, dataSource, navigate, setGuard]);

  // 监听浏览器返回/刷新操作
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      e.returnValue = '';
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, []);


  // 处理表格数据变化的包装函数
  const handleDataSourceChange = useCallback((value: readonly PrepaymentItem[]) => {
    console.log('handleDataSourceChange 被调用，value:', value)
    const next = [...value] as PrepaymentItem[];
    setDataSource(next);
    
    console.log('表格数据已更新:', next);
  }, []);

  // PDF 加载成功回调
  const onDocumentLoadSuccess = useCallback(({ numPages }: { numPages: number }) => {
    console.log('PDF 加载成功，numPages:', numPages);
    setNumPages(numPages);
    setPdfLoading(false);
  }, []);

  // 监听左侧面板宽度变化，更新 PDF 宽度
  useEffect(() => {
    const updatePdfWidth = () => {
      if (leftPanelRef.current) {
        const width = leftPanelRef.current.offsetWidth;
        setPdfWidth(Math.max(width - 80, 300)); // 减去 padding，最小 300px
      }
    };
    
    // 使用 requestAnimationFrame 避免闪烁
    const rafId = requestAnimationFrame(updatePdfWidth);
    window.addEventListener('resize', updatePdfWidth);
    
    return () => {
      cancelAnimationFrame(rafId);
      window.removeEventListener('resize', updatePdfWidth);
    };
  }, [leftWidth]);

  // 处理分割线拖动
  const handleMouseDown = useCallback(() => {
    setIsDragging(true);
  }, []);

  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (!isDragging || !containerRef.current) return;
    
    const containerRect = containerRef.current.getBoundingClientRect();
    const newLeftWidth = ((e.clientX - containerRect.left) / containerRect.width) * 100;
    
    // 限制最小宽度为 30%，最大为 70%
    if (newLeftWidth >= 30 && newLeftWidth <= 70) {
      setLeftWidth(newLeftWidth);
    }
  }, [isDragging]);

  const handleMouseUp = useCallback(() => {
    setIsDragging(false);
  }, []);

  // 监听拖动事件
  useEffect(() => {
    if (isDragging) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      
      return () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
      };
    }
  }, [isDragging, handleMouseMove, handleMouseUp]);

  if (!contractInfo) {
    return null;
  }

  return (
    <div className={styles.splitContainer} ref={containerRef}>
      {/* 左侧 PDF 预览区 */}
      <div 
        className={styles.leftPanel} 
        ref={leftPanelRef}
        style={{ width: `${leftWidth}%` }}
      >
        <div className={styles.pdfHeader}>
          <h3>合同预览</h3>
          {numPages > 0 && (
            <span className={styles.pageInfo}>
              共 {numPages} 页
            </span>
          )}
        </div>
        <div className={styles.pdfViewer}>
          <Document
            file={pdfUrl || pdfFile}
            onLoadSuccess={onDocumentLoadSuccess}
            onLoadError={() => setPdfLoading(false)}
            loading={(
              <div className={styles.pdfLoadingSkeleton}>
                <Skeleton active paragraph={{ rows: 10 }} />
                <Skeleton active paragraph={{ rows: 10 }} style={{ marginTop: 20 }} />
                <Skeleton active paragraph={{ rows: 10 }} style={{ marginTop: 20 }} />
              </div>
            )}
            error={<div className={styles.pdfError}>PDF 加载失败</div>}
          >
            {Array.from(new Array(numPages), (_, index) => (
              <Page
                key={`page_${index + 1}`}
                pageNumber={index + 1}
                width={pdfWidth}
                renderTextLayer={false}
                renderAnnotationLayer={false}
              />
            ))}
          </Document>
        </div>
      </div>

      {/* 分割线 */}
      <div 
        className={`${styles.divider} ${isDragging ? styles.dividerActive : ''}`}
        onMouseDown={handleMouseDown}
      />

      {/* 右侧合同确认区 */}
      <div className={styles.rightPanel} style={{ width: `${100 - leftWidth}%` }}>
        <Card 
          title="合同确认" 
          className={styles.pageCard}
          extra={
            <div className={styles.actionButtons}>
              <Button onClick={handleCancel} disabled={submitting}>
                取消并返回
              </Button>
              <Button type="primary" onClick={handleConfirm} loading={submitting}>
                确认
              </Button>
            </div>
          }
        >
          <div className={styles.contentContainer}>
            {/* 合同信息部分 */}
            <div className={styles.contractInfo}>
              <Descriptions
                bordered
                column={2}
                size="small"
                labelStyle={{ fontWeight: 'bold', width: '120px' }}
                contentStyle={{ textAlign: 'left' }}
              >
                <Descriptions.Item label="合同附件名称" span={2}>
                  <Input
                    value={editableContractInfo.attachmentName}
                    onChange={(e) => {
                      setEditableContractInfo({
                        ...editableContractInfo,
                        attachmentName: e.target.value
                      });
                      // 清除错误状态
                      if (validationErrors.attachmentName) {
                        setValidationErrors({ ...validationErrors, attachmentName: false });
                      }
                    }}
                    placeholder="请输入合同附件名称"
                    status={validationErrors.attachmentName ? 'error' : ''}
                  />
                </Descriptions.Item>
                <Descriptions.Item label="供应商名称">
                  <Input
                    value={editableContractInfo.vendorName}
                    onChange={(e) => {
                      setEditableContractInfo({
                        ...editableContractInfo,
                        vendorName: e.target.value
                      });
                      if (validationErrors.vendorName) {
                        setValidationErrors({ ...validationErrors, vendorName: false });
                      }
                    }}
                    placeholder="请输入供应商名称"
                    status={validationErrors.vendorName ? 'error' : ''}
                  />
                </Descriptions.Item>
                <Descriptions.Item label="合同总金额">
                  <InputNumber
                    value={editableContractInfo.totalAmount}
                    onChange={(value) => {
                      setEditableContractInfo({
                        ...editableContractInfo,
                        totalAmount: value || 0
                      });
                      if (validationErrors.totalAmount) {
                        setValidationErrors({ ...validationErrors, totalAmount: false });
                      }
                    }}
                    prefix="¥"
                    precision={2}
                    min={0}
                    style={{ width: '100%' }}
                    placeholder="请输入合同总金额"
                    status={validationErrors.totalAmount ? 'error' : ''}
                  />
                </Descriptions.Item>
                <Descriptions.Item label="合同开始时间">
                  <DatePicker
                    value={editableContractInfo.startDate ? dayjs(editableContractInfo.startDate) : null}
                    onChange={(date) => {
                      setEditableContractInfo({
                        ...editableContractInfo,
                        startDate: date ? date.format('YYYY-MM-DD') : ''
                      });
                      if (validationErrors.startDate) {
                        setValidationErrors({ ...validationErrors, startDate: false });
                      }
                    }}
                    format="YYYY-MM-DD"
                    style={{ width: '100%' }}
                    placeholder="请选择开始时间"
                    inputReadOnly
                    status={validationErrors.startDate ? 'error' : ''}
                  />
                </Descriptions.Item>
                <Descriptions.Item label="合同结束时间">
                  <DatePicker
                    value={editableContractInfo.endDate ? dayjs(editableContractInfo.endDate) : null}
                    onChange={(date) => {
                      setEditableContractInfo({
                        ...editableContractInfo,
                        endDate: date ? date.format('YYYY-MM-DD') : ''
                      });
                      if (validationErrors.endDate) {
                        setValidationErrors({ ...validationErrors, endDate: false });
                      }
                    }}
                    format="YYYY-MM-DD"
                    style={{ width: '100%' }}
                    placeholder="请选择结束时间"
                    inputReadOnly
                    status={validationErrors.endDate ? 'error' : ''}
                  />
                </Descriptions.Item>
                <Descriptions.Item label="税率">
                  <InputNumber
                    value={editableContractInfo.taxRate ? editableContractInfo.taxRate * 100 : 0}
                    onChange={(value) => {
                      setEditableContractInfo({
                        ...editableContractInfo,
                        taxRate: value ? value / 100 : 0
                      });
                      if (validationErrors.taxRate) {
                        setValidationErrors({ ...validationErrors, taxRate: false });
                      }
                    }}
                    suffix="%"
                    precision={2}
                    min={0}
                    max={100}
                    style={{ width: '100%' }}
                    placeholder="请输入税率"
                    status={validationErrors.taxRate ? 'error' : ''}
                  />
                </Descriptions.Item>
                <Descriptions.Item label="创建时间">
                  {formatDateTime(contractInfo.createdAt)}
                </Descriptions.Item>
                
                {/* 自定义字段展示 */}
                {editableContractInfo.customFields && Object.entries(editableContractInfo.customFields).map(([key, value]) => (
                  <Descriptions.Item label={key} span={2} key={key}>
                    <Input
                      value={String(value || '')}
                      onChange={(e) => {
                        setEditableContractInfo({
                          ...editableContractInfo,
                          customFields: {
                            ...editableContractInfo.customFields,
                            [key]: e.target.value
                          }
                        });
                      }}
                      placeholder={`请输入${key}`}
                    />
                  </Descriptions.Item>
                ))}
              </Descriptions>
            </div>

            {/* 摊销明细表部分 */}
            <div className={styles.prepaymentTable}>
              <EditableProTable<PrepaymentItem>
                rowKey="id"
                headerTitle="摊销明细表"
                columns={columns}
                value={dataSource}
                onChange={handleDataSourceChange}
                recordCreatorProps={{
                  position: 'bottom',
                  newRecordType: 'dataSource',
                  record: () => {
                    const tempId = generateTempId();
                    const currentDate = new Date();
                    const formattedDate = currentDate.toISOString().split('T')[0];
                    
                    const newRecord: PrepaymentItem = {
                      id: tempId,
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
                    .filter(item => item.id != null)
                    .map(item => String(item.id)),
                  actionRender: (_row, _config, defaultDom) => [
                    defaultDom.delete,
                  ],
                  onSave: handleSave,
                  onChange: (editableKeys) => {
                    console.log('编辑状态变化:', editableKeys);
                  },
                  onValuesChange: (record, recordList) => {
                    console.log('onValuesChange 被调用:', { record, recordList });
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
        </Card>
      </div>
    </div>
  );
};

export default ContractPreview;
