import React, { useState, useCallback, useMemo, useEffect, useRef } from 'react';
import { Button, message, Descriptions, Card, Modal } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useNavigationGuard } from '../../contexts/NavigationGuardContext';
import { EditableProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Document, Page, pdfjs } from 'react-pdf';
import { PrepaymentItem } from './types';
import styles from './styles.module.css';
import pdfFile from '../../constants/contract_20251015_200057_504d8439.pdf?url';

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
  
  // 从路由状态中获取合同信息和摊销数据
  const { contractInfo, prepaymentData } = location.state || {};
  
  // 表格数据状态管理
  const [dataSource, setDataSource] = useState<PrepaymentItem[]>(prepaymentData || []);
  // 提交状态
  const [submitting] = useState(false);
  // PDF 相关状态
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
  ], [handleDeleteRow]);

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

  // 确认按钮处理 - 直接返回列表页（不需要确认）
  const handleConfirm = useCallback(() => {
    setGuard(null); // 移除守卫
    navigate('/page-a');
  }, [navigate, setGuard]);

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
    setNumPages(numPages);
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
            file={pdfFile}
            onLoadSuccess={onDocumentLoadSuccess}
            loading={<div className={styles.pdfLoading}>加载 PDF 中...</div>}
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
