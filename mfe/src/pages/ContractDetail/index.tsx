import React, { useState, useEffect } from 'react';
import { Typography, Table, Tabs, Spin, message, Button, Modal, InputNumber, Space } from 'antd';
import { getContractAmortizationEntries, ContractAmortizationResponse, ContractAmortizationEntry, executePayment, PaymentExecuteRequest } from '../../api/contracts';
import { getJournalEntriesPreview, JournalEntriesPreviewResponse, DateRangeFilter, SortConfig } from '../../api/journalEntries';

const { Title, Text } = Typography;

const ContractDetail: React.FC = () => {
  const [contractData, setContractData] = useState<ContractAmortizationResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [activeKey, setActiveKey] = useState('timeline');
  
  // 支付弹窗相关状态
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [currentEditRecord, setCurrentEditRecord] = useState<ContractAmortizationEntry | null>(null);
  const [newAmount, setNewAmount] = useState<number | null>(null);
  
  // 批量编辑弹窗相关状态
  const [isBatchModalVisible, setIsBatchModalVisible] = useState(false);
  const [batchNewAmount, setBatchNewAmount] = useState<number | null>(null);
  
  // 多选相关状态
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  
  // 支付加载状态
  const [paymentLoading, setPaymentLoading] = useState(false);
  
  
  // 预提会计分录预览数据
  const [journalEntriesData, setJournalEntriesData] = useState<JournalEntriesPreviewResponse | null>(null);
  const [journalEntriesLoading, setJournalEntriesLoading] = useState(false);
  
  // 付款会计分录预览数据
  const [paymentJournalEntriesData, setPaymentJournalEntriesData] = useState<JournalEntriesPreviewResponse | null>(null);
  const [paymentJournalEntriesLoading, setPaymentJournalEntriesLoading] = useState(false);
  
  // 预提会计分录筛选和排序状态
  const [dateRangeFilter, setDateRangeFilter] = useState<DateRangeFilter>({});
  const [sortConfig, setSortConfig] = useState<SortConfig>({ field: 'entryOrder', order: 'asc' });
  
  // 是否为初始加载
  const [isInitialLoad, setIsInitialLoad] = useState(true);

  // 占位：后续从路由参数获取真实contractId
  const contractId = 1;

  // 获取合同摊销明细数据
  const fetchContractData = async () => {
    setLoading(true);
    try {
      const response = await getContractAmortizationEntries(contractId);
      setContractData(response);
    } catch (error) {
      message.error('获取合同摊销明细失败');
      console.error('API调用失败:', error);
    } finally {
      setLoading(false);
    }
  };


  // 获取预提会计分录预览数据
  const fetchJournalEntriesPreview = async () => {
    setJournalEntriesLoading(true);
    try {
      const response = await getJournalEntriesPreview({
        contractId,
        previewType: 'AMORTIZATION'
      });
      setJournalEntriesData(response);
    } catch (error) {
      message.error('获取预提会计分录预览失败');
      console.error('API调用失败:', error);
    } finally {
      setJournalEntriesLoading(false);
    }
  };

  // 获取付款会计分录预览数据
  const fetchPaymentJournalEntriesPreview = async () => {
    setPaymentJournalEntriesLoading(true);
    try {
      const response = await getJournalEntriesPreview({
        contractId,
        previewType: 'PAYMENT'
      });
      setPaymentJournalEntriesData(response);
    } catch (error) {
      message.error('获取付款会计分录预览失败');
      console.error('API调用失败:', error);
    } finally {
      setPaymentJournalEntriesLoading(false);
    }
  };

  // 组件加载时获取数据
  useEffect(() => {
    fetchContractData();
    setIsInitialLoad(false); // 初始加载完成
  }, [contractId]);

  // Tab切换时获取对应数据
  useEffect(() => {
    // 跳过初始加载时的Tab切换逻辑
    if (isInitialLoad) return;

    const handleTabDataLoading = async () => {
      if (activeKey === 'accrual') {
        await fetchJournalEntriesPreview();
      } else if (activeKey === 'timeline') {
        // 预提支付页面，检查数据是否存在，需要时重新加载
        if (!contractData) {
          await fetchContractData();
        }
      } else if (activeKey === 'payment') {
        // 付款会计分录页面，调用真实API
        await fetchPaymentJournalEntriesPreview();
      }
    };

    // 执行Tab切换加载逻辑
    handleTabDataLoading();
  }, [activeKey, isInitialLoad]);

  // 计算摊销周期
  const calculateAmortizationPeriods = () => {
    if (!contractData?.contract) return '';
    const startDate = new Date(contractData.contract.startDate);
    const endDate = new Date(contractData.contract.endDate);
    const monthDiff = (endDate.getFullYear() - startDate.getFullYear()) * 12 + 
                      (endDate.getMonth() - startDate.getMonth()) + 1;
    return `共 ${monthDiff} 期，按月摊销`;
  };

  // 打开支付弹窗
  const handleEditAmount = (record: ContractAmortizationEntry) => {
    setCurrentEditRecord(record);
    setNewAmount(record.amount);
    setIsModalVisible(true);
  };

  // 确认支付
  const handleConfirmEdit = async () => {
    if (!currentEditRecord || newAmount === null) {
      message.warning('请输入有效金额');
      return;
    }

    setPaymentLoading(true);
    try {
      // 构造支付请求参数
      const paymentRequest: PaymentExecuteRequest = {
        contractId,
        paymentAmount: newAmount,
        bookingDate: new Date().toISOString().split('T')[0], // 当前日期 YYYY-MM-DD
        selectedPeriods: [currentEditRecord.id] // 选中的期次
      };

      // 调用支付接口
      const response = await executePayment(paymentRequest);
      
      // 显示支付结果
      message.success(response.message || '付款执行成功');
      
      // 关闭弹窗并重置状态
      setIsModalVisible(false);
      setCurrentEditRecord(null);
      setNewAmount(null);
      
      // 重新获取数据以更新状态
      await fetchContractData();
    } catch (error) {
      message.error('支付失败，请重试');
      console.error('支付失败:', error);
    } finally {
      setPaymentLoading(false);
    }
  };

  // 取消支付
  const handleCancelEdit = () => {
    setIsModalVisible(false);
    setCurrentEditRecord(null);
    setNewAmount(null);
  };

  // 打开批量编辑弹窗
  const handleBatchEdit = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要编辑的项目');
      return;
    }
    setBatchNewAmount(null);
    setIsBatchModalVisible(true);
  };

  // 确认批量支付
  const handleConfirmBatchEdit = async () => {
    if (batchNewAmount === null) {
      message.warning('请输入有效金额');
      return;
    }

    setPaymentLoading(true);
    try {
      // 构造批量支付请求参数
      const paymentRequest: PaymentExecuteRequest = {
        contractId,
        paymentAmount: batchNewAmount,
        bookingDate: new Date().toISOString().split('T')[0], // 当前日期 YYYY-MM-DD
        selectedPeriods: selectedRowKeys.map(key => Number(key)) // 选中的期次
      };

      // 调用支付接口
      const response = await executePayment(paymentRequest);
      
      // 显示支付结果
      message.success(response.message || '批量付款执行成功');
      
      // 关闭弹窗并重置状态
      setIsBatchModalVisible(false);
      setBatchNewAmount(null);
      setSelectedRowKeys([]); // 清空选择
      
      // 重新获取数据以更新状态
      await fetchContractData();
    } catch (error) {
      message.error('批量支付失败，请重试');
      console.error('批量支付失败:', error);
    } finally {
      setPaymentLoading(false);
    }
  };

  // 取消批量支付
  const handleCancelBatchEdit = () => {
    setIsBatchModalVisible(false);
    setBatchNewAmount(null);
  };

  // 获取选中的记录
  const getSelectedRecords = (): ContractAmortizationEntry[] => {
    if (!contractData?.amortization) return [];
    return contractData.amortization.filter(record => selectedRowKeys.includes(record.id));
  };

  // 多选处理
  const rowSelection = {
    selectedRowKeys,
    onChange: (newSelectedRowKeys: React.Key[]) => {
      setSelectedRowKeys(newSelectedRowKeys);
    },
    onSelectAll: (selected: boolean, selectedRows: ContractAmortizationEntry[], changeRows: ContractAmortizationEntry[]) => {
      console.log('Select all:', selected, selectedRows, changeRows);
    },
    onSelect: (record: ContractAmortizationEntry, selected: boolean, selectedRows: ContractAmortizationEntry[]) => {
      console.log('Select:', record, selected, selectedRows);
    },
  };

  // 预提支付表表头（匹配API返回数据）
  const columns = [
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>期次</span>, 
      dataIndex: 'id', 
      key: 'id', 
      width: 80 
    },
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>预提期间</span>, 
      dataIndex: 'amortizationPeriod', 
      key: 'amortizationPeriod', 
      width: 120 
    },
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>会计期间</span>, 
      dataIndex: 'accountingPeriod', 
      key: 'accountingPeriod', 
      width: 120 
    },
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>期间日期</span>, 
      dataIndex: 'periodDate', 
      key: 'periodDate', 
      width: 120 
    },
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>金额（元）</span>, 
      dataIndex: 'amount', 
      key: 'amount', 
      width: 120,
      render: (amount: number) => (
        <span style={{ color: '#E31E24', fontWeight: '600', fontSize: '14px' }}>
          ¥{amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
        </span>
      )
    },
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>付款状态</span>, 
      dataIndex: 'paymentStatus', 
      key: 'paymentStatus', 
      width: 100,
      render: (status: string) => {
        const statusMap: Record<string, { text: string; color: string; bgColor: string }> = {
          'PENDING': { text: '待付款', color: '#B45309', bgColor: '#FEF3C7' },
          'PAID': { text: '已付款', color: '#065F46', bgColor: '#D1FAE5' },
          'OVERDUE': { text: '逾期', color: '#DC2626', bgColor: '#FEE2E2' }
        };
        const statusInfo = statusMap[status] || { text: status, color: '#6B7280', bgColor: '#F3F4F6' };
        return (
          <span style={{ 
            color: statusInfo.color, 
            fontWeight: '600',
            fontSize: '13px',
            padding: '4px 8px',
            borderRadius: '4px',
            backgroundColor: statusInfo.bgColor
          }}>
            {statusInfo.text}
          </span>
        );
      }
    },
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>创建时间</span>, 
      dataIndex: 'createdAt', 
      key: 'createdAt', 
      width: 180,
      render: (time: string) => (
        <span style={{ color: '#6B7280', fontSize: '13px' }}>{time}</span>
      )
    },
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>操作</span>, 
      key: 'action', 
      width: 120, 
      render: (_: any, record: ContractAmortizationEntry) => (
        <Button 
          type="primary"
          size="small" 
          onClick={() => handleEditAmount(record)}
          style={{
            backgroundColor: '#E31E24',
            borderColor: '#E31E24',
            color: '#FFFFFF',
            fontWeight: '600',
            fontSize: '13px',
            borderRadius: '8px'
          }}
          onMouseEnter={(e) => {
            (e.target as HTMLElement).style.backgroundColor = '#C41E3A';
            (e.target as HTMLElement).style.borderColor = '#C41E3A';
          }}
          onMouseLeave={(e) => {
            (e.target as HTMLElement).style.backgroundColor = '#E31E24';
            (e.target as HTMLElement).style.borderColor = '#E31E24';
          }}
        >
          支付
        </Button>
      )
    },
  ];

  // 会计分录明细表头
  const journalEntryColumns = [
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>记账日期</span>, 
      dataIndex: 'bookingDate', 
      key: 'bookingDate', 
      width: 120,
      render: (date: string) => (
        <span style={{ color: '#6B7280', fontSize: '13px' }}>{date}</span>
      )
    },
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>会计科目</span>, 
      dataIndex: 'account', 
      key: 'account', 
      width: 120,
      render: (account: string) => (
        <span style={{ color: '#1F2937', fontSize: '13px', fontWeight: '500' }}>{account}</span>
      )
    },
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>借方金额</span>, 
      dataIndex: 'dr', 
      key: 'dr', 
      width: 120,
      render: (amount: number) => amount > 0 ? (
        <span style={{ color: '#E31E24', fontWeight: '600', fontSize: '13px' }}>¥{amount.toFixed(2)}</span>
      ) : (
        <span style={{ color: '#9CA3AF', fontSize: '13px' }}>-</span>
      )
    },
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>贷方金额</span>, 
      dataIndex: 'cr', 
      key: 'cr', 
      width: 120,
      render: (amount: number) => amount > 0 ? (
        <span style={{ color: '#E31E24', fontWeight: '600', fontSize: '13px' }}>¥{amount.toFixed(2)}</span>
      ) : (
        <span style={{ color: '#9CA3AF', fontSize: '13px' }}>-</span>
      )
    },
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>备注</span>, 
      dataIndex: 'memo', 
      key: 'memo', 
      width: 150,
      render: (memo: string) => (
        <span style={{ color: '#6B7280', fontSize: '13px' }}>{memo}</span>
      )
    },
  ];

  const paymentColumns = [
    { title: '付款日期', dataIndex: 'paymentDate', key: 'paymentDate' },
    { title: '科目', dataIndex: 'account', key: 'account' },
    { title: '金额', dataIndex: 'amount', key: 'amount' },
    { title: '备注', dataIndex: 'remark', key: 'remark' },
  ];


  const getColumnsByKey = (key: string) => {
    if (key === 'accrual') return journalEntryColumns as any;
    if (key === 'payment') return paymentColumns as any;
    return columns as any;
  };

  const getDataSourceByKey = (key: string) => {
    if (key === 'timeline' && contractData) {
      return contractData.amortization;
    }
    // 其他Tab暂时返回空数组
    return [];
  };

  // 处理预提分录数据的排序和筛选
  const getFilteredAndSortedEntries = () => {
    if (!journalEntriesData?.previewEntries) return [];
    
    let filteredEntries = [...journalEntriesData.previewEntries];
    
    // 日期范围筛选
    if (dateRangeFilter.startDate || dateRangeFilter.endDate) {
      filteredEntries = filteredEntries.filter(entry => {
        const entryDate = new Date(entry.bookingDate);
        const startDate = dateRangeFilter.startDate ? new Date(dateRangeFilter.startDate) : null;
        const endDate = dateRangeFilter.endDate ? new Date(dateRangeFilter.endDate) : null;
        
        if (startDate && entryDate < startDate) return false;
        if (endDate && entryDate > endDate) return false;
        return true;
      });
    }
    
    // 排序
    filteredEntries.sort((a, b) => {
      const aValue = sortConfig.field === 'entryOrder' ? a.entryOrder : new Date(a.bookingDate).getTime();
      const bValue = sortConfig.field === 'entryOrder' ? b.entryOrder : new Date(b.bookingDate).getTime();
      
      return sortConfig.order === 'asc' ? aValue - bValue : bValue - aValue;
    });
    
    return filteredEntries;
  };

  // 预提分录列表表格列定义
  const previewEntriesColumns = [
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>分录顺序</span>,
      dataIndex: 'entryOrder',
      key: 'entryOrder',
      width: 100,
      align: 'center' as const,
      render: (order: number) => (
        <span style={{ color: '#1F2937', fontSize: '13px', fontWeight: '500' }}>{order}</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>业务类型</span>,
      dataIndex: 'entryType',
      key: 'entryType',
      width: 100,
      align: 'center' as const,
      render: (type: string) => (
        <span style={{ color: '#1F2937', fontSize: '13px', fontWeight: '500' }}>
          {type === 'AMORTIZATION' ? '摊销' : type}
        </span>
      )
    },
    {
      title: (
        <span 
          style={{ 
            color: '#0F172A', 
            fontWeight: '600', 
            fontSize: '14px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
          onClick={() => {
            const newOrder = sortConfig.field === 'bookingDate' && sortConfig.order === 'asc' ? 'desc' : 'asc';
            setSortConfig({ field: 'bookingDate', order: newOrder });
          }}
        >
          入账日期 {sortConfig.field === 'bookingDate' && (sortConfig.order === 'asc' ? '↑' : '↓')}
        </span>
      ),
      dataIndex: 'bookingDate',
      key: 'bookingDate',
      width: 120,
      align: 'center' as const,
      render: (date: string) => (
        <span style={{ color: '#1F2937', fontSize: '13px', fontWeight: '500' }}>{date}</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>会计科目</span>,
      dataIndex: 'accountName',
      key: 'accountName',
      width: 120,
      render: (account: string) => (
        <span style={{ color: '#1F2937', fontSize: '13px', fontWeight: '500' }}>{account}</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>借方金额</span>,
      dataIndex: 'debitAmount',
      key: 'debitAmount',
      width: 120,
      align: 'right' as const,
      render: (amount: number) => amount > 0 ? (
        <span style={{ color: '#E31E24', fontWeight: '600', fontSize: '13px' }}>¥{amount.toFixed(2)}</span>
      ) : (
        <span style={{ color: '#9CA3AF', fontSize: '13px' }}>-</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>贷方金额</span>,
      dataIndex: 'creditAmount',
      key: 'creditAmount',
      width: 120,
      align: 'right' as const,
      render: (amount: number) => amount > 0 ? (
        <span style={{ color: '#E31E24', fontWeight: '600', fontSize: '13px' }}>¥{amount.toFixed(2)}</span>
      ) : (
        <span style={{ color: '#9CA3AF', fontSize: '13px' }}>-</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>分录描述</span>,
      dataIndex: 'description',
      key: 'description',
      width: 150,
      render: (description: string) => (
        <span style={{ color: '#1F2937', fontSize: '13px' }}>{description}</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>备注</span>,
      dataIndex: 'memo',
      key: 'memo',
      width: 200,
      render: (memo: string) => (
        <span style={{ color: '#6B7280', fontSize: '13px' }}>{memo}</span>
      )
    }
  ];

  // 渲染预提会计分录页面
  const renderAccrualRecords = () => {
    if (journalEntriesLoading) {
      return (
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Spin size="large" className="outlook-spin" />
          <div style={{ marginTop: 16 }}>
            <Text style={{ color: '#6B7280', fontSize: '14px' }}>正在加载预提会计分录数据...</Text>
          </div>
        </div>
      );
    }

    if (!journalEntriesData) {
      return (
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Text type="secondary">暂无预提会计分录数据</Text>
        </div>
      );
    }

    const filteredEntries = getFilteredAndSortedEntries();

    return (
      <div>
        {/* 合同信息区 */}
        {/* <div style={{
          marginBottom: 24,
          padding: '16px 20px',
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          border: '1px solid #E5E5E5',
          borderLeft: '4px solid #E31E24'
        }}>
          <div style={{ marginBottom: '12px' }}>
            <Text style={{ 
              color: '#1F2937', 
              fontSize: '16px',
              fontWeight: '600',
              fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
            }}>
              关联合同信息
            </Text>
          </div>
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', 
            gap: '16px',
            alignItems: 'start'
          }}>
            <div>
              <div style={{ 
                color: '#6B7280', 
                fontSize: '14px',
                fontWeight: '600',
                marginBottom: '6px',
                fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
              }}>
                供应商名称：
              </div>
              <div style={{ 
                color: '#1F2937', 
                fontSize: '14px',
                fontWeight: '400',
                fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
              }}>
                {journalEntriesData.contract.vendorName}
              </div>
            </div>
            <div>
              <div style={{ 
                color: '#6B7280', 
                fontSize: '14px',
                fontWeight: '600',
                marginBottom: '6px',
                fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
              }}>
                合同总金额：
              </div>
              <div style={{ 
                color: '#E31E24', 
                fontSize: '14px',
                fontWeight: '600',
                fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
              }}>
                ¥{journalEntriesData.contract.totalAmount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} 元
              </div>
            </div>
            <div>
              <div style={{ 
                color: '#6B7280', 
                fontSize: '14px',
                fontWeight: '600',
                marginBottom: '6px',
                fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
              }}>
                合同周期：
              </div>
              <div style={{ 
                color: '#1F2937', 
                fontSize: '14px',
                fontWeight: '400',
                fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
              }}>
                {journalEntriesData.contract.startDate} 至 {journalEntriesData.contract.endDate}
              </div>
            </div>
          </div>
        </div> */}

        {/* 预提分录列表 */}
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          border: '1px solid #E5E5E5',
          overflow: 'hidden'
        }}>
          <Table
            columns={previewEntriesColumns}
            dataSource={filteredEntries}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条记录`
            }}
            size="middle"
            rowKey="entryOrder"
            scroll={{ x: 1000 }}
          />
        </div>
      </div>
    );
  };

  // 处理付款分录数据的排序和筛选
  const getFilteredAndSortedPaymentEntries = () => {
    if (!paymentJournalEntriesData?.previewEntries) return [];
    
    let filteredEntries = [...paymentJournalEntriesData.previewEntries];
    
    // 日期范围筛选
    if (dateRangeFilter.startDate || dateRangeFilter.endDate) {
      filteredEntries = filteredEntries.filter(entry => {
        const entryDate = new Date(entry.bookingDate);
        const startDate = dateRangeFilter.startDate ? new Date(dateRangeFilter.startDate) : null;
        const endDate = dateRangeFilter.endDate ? new Date(dateRangeFilter.endDate) : null;
        
        if (startDate && entryDate < startDate) return false;
        if (endDate && entryDate > endDate) return false;
        return true;
      });
    }
    
    // 排序
    filteredEntries.sort((a, b) => {
      const aValue = sortConfig.field === 'entryOrder' ? a.entryOrder : new Date(a.bookingDate).getTime();
      const bValue = sortConfig.field === 'entryOrder' ? b.entryOrder : new Date(b.bookingDate).getTime();
      
      return sortConfig.order === 'asc' ? aValue - bValue : bValue - aValue;
    });
    
    return filteredEntries;
  };

  // 付款分录列表表格列定义
  const paymentEntriesColumns = [
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>分录顺序</span>,
      dataIndex: 'entryOrder',
      key: 'entryOrder',
      width: 100,
      align: 'center' as const,
      render: (order: number) => (
        <span style={{ color: '#1F2937', fontSize: '13px', fontWeight: '500' }}>{order}</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>业务类型</span>,
      dataIndex: 'entryType',
      key: 'entryType',
      width: 100,
      align: 'center' as const,
      render: (type: string) => (
        <span style={{ color: '#1F2937', fontSize: '13px', fontWeight: '500' }}>
          {type === 'PAYMENT' ? '付款' : type}
        </span>
      )
    },
    {
      title: (
        <span 
          style={{ 
            color: '#0F172A', 
            fontWeight: '600', 
            fontSize: '14px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
          onClick={() => {
            const newOrder = sortConfig.field === 'bookingDate' && sortConfig.order === 'asc' ? 'desc' : 'asc';
            setSortConfig({ field: 'bookingDate', order: newOrder });
          }}
        >
          入账日期 {sortConfig.field === 'bookingDate' && (sortConfig.order === 'asc' ? '↑' : '↓')}
        </span>
      ),
      dataIndex: 'bookingDate',
      key: 'bookingDate',
      width: 120,
      align: 'center' as const,
      render: (date: string) => (
        <span style={{ color: '#1F2937', fontSize: '13px', fontWeight: '500' }}>{date}</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>会计科目</span>,
      dataIndex: 'accountName',
      key: 'accountName',
      width: 120,
      render: (account: string) => (
        <span style={{ color: '#1F2937', fontSize: '13px', fontWeight: '500' }}>{account}</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>借方金额</span>,
      dataIndex: 'debitAmount',
      key: 'debitAmount',
      width: 120,
      align: 'right' as const,
      render: (amount: number) => amount > 0 ? (
        <span style={{ color: '#E31E24', fontWeight: '600', fontSize: '13px' }}>¥{amount.toFixed(2)}</span>
      ) : (
        <span style={{ color: '#9CA3AF', fontSize: '13px' }}>-</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>贷方金额</span>,
      dataIndex: 'creditAmount',
      key: 'creditAmount',
      width: 120,
      align: 'right' as const,
      render: (amount: number) => amount > 0 ? (
        <span style={{ color: '#E31E24', fontWeight: '600', fontSize: '13px' }}>¥{amount.toFixed(2)}</span>
      ) : (
        <span style={{ color: '#9CA3AF', fontSize: '13px' }}>-</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>分录描述</span>,
      dataIndex: 'description',
      key: 'description',
      width: 150,
      render: (description: string) => (
        <span style={{ color: '#1F2937', fontSize: '13px' }}>{description}</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>备注</span>,
      dataIndex: 'memo',
      key: 'memo',
      width: 200,
      render: (memo: string) => (
        <span style={{ color: '#6B7280', fontSize: '13px' }}>{memo}</span>
      )
    }
  ];

  // 渲染付款会计分录页面
  const renderPaymentRecords = () => {
    if (paymentJournalEntriesLoading) {
      return (
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Spin size="large" className="outlook-spin" />
          <div style={{ marginTop: 16 }}>
            <Text style={{ color: '#6B7280', fontSize: '14px' }}>正在加载付款会计分录数据...</Text>
          </div>
        </div>
      );
    }

    if (!paymentJournalEntriesData) {
      return (
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Text type="secondary">暂无付款会计分录数据</Text>
        </div>
      );
    }

    const filteredEntries = getFilteredAndSortedPaymentEntries();

    return (
      <div>
        {/* 付款分录列表 */}
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          border: '1px solid #E5E5E5',
          overflow: 'hidden'
        }}>
          <Table
            columns={paymentEntriesColumns}
            dataSource={filteredEntries}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条记录`
            }}
            size="middle"
            rowKey="entryOrder"
            scroll={{ x: 1000 }}
          />
        </div>
      </div>
    );
  };

  const contractName = contractData?.contract?.vendorName || '加载中...';

  return (
    <>
      <style>
        {`
          .outlook-spin .ant-spin-dot-item {
            background-color: #E31E24 !important;
          }
          .outlook-spin .ant-spin-dot {
            color: #E31E24 !important;
          }
          .outlook-spin .ant-spin-spinning .ant-spin-dot-item {
            background-color: #E31E24 !important;
          }
          .ant-tabs .ant-tabs-ink-bar {
            background-color: #E31E24 !important;
          }
        `}
      </style>
      <div style={{ 
        padding: 24, 
        backgroundColor: '#FFFFFF',
        minHeight: '100vh'
      }}>
      {/* 合同详情标题 */}
      <div style={{
        backgroundColor: 'transparent',
        marginBottom: 24,
        padding: 0
      }}>
        <Title 
          level={4} 
          style={{ 
            marginBottom: 0,
            color: '#333333',
            fontSize: '20px',
            fontWeight: '600',
            letterSpacing: '0.5px'
          }}
        >
          合同详情
        </Title>
      </div>

      {/* 合同基本信息区 */}
      <div style={{ 
        marginBottom: 24, 
        padding: '16px 20px',
        backgroundColor: '#FFFFFF',
        borderRadius: '12px',
        border: '1px solid #E5E5E5',
        borderLeft: '4px solid #E31E24'
      }}>
        <div style={{ marginBottom: '16px' }}>
          <Text style={{ 
            color: '#1F2937', 
            fontSize: '16px',
            fontWeight: '600',
            fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
          }}>
            合同基本信息
          </Text>
        </div>
        <Spin 
          spinning={loading}
          className="outlook-spin"
        >
          {contractData?.contract ? (
            <>
              {/* 紧凑型网格布局 - 四列横向排列 */}
              <div style={{ 
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '20px',
                marginBottom: '16px'
              }}>
                {/* 合同ID */}
                <div>
                  <div style={{ 
                    color: '#6B7280', 
                    fontSize: '14px',
                    fontWeight: '600',
                    marginBottom: '6px',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    合同 ID：
                  </div>
                  <div style={{ 
                    color: '#1F2937', 
                    fontSize: '14px',
                    fontWeight: '400',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    {contractData.contract.id}
                  </div>
                </div>

                {/* 供应商名称 */}
                <div>
                  <div style={{ 
                    color: '#6B7280', 
                    fontSize: '14px',
                    fontWeight: '600',
                    marginBottom: '6px',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    供应商名称：
                  </div>
                  <div style={{ 
                    color: '#1F2937', 
                    fontSize: '14px',
                    fontWeight: '400',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    {contractData.contract.vendorName}
                  </div>
                </div>

                {/* 合同期限 */}
                <div>
                  <div style={{ 
                    color: '#6B7280', 
                    fontSize: '14px',
                    fontWeight: '600',
                    marginBottom: '6px',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    合同期限：
                  </div>
                  <div style={{ 
                    color: '#1F2937', 
                    fontSize: '14px',
                    fontWeight: '400',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif',
                    lineHeight: '1.5'
                  }}>
                    {contractData.contract.startDate} 至 {contractData.contract.endDate}
                  </div>
                </div>

                {/* 合同总金额 */}
                <div>
                  <div style={{ 
                    color: '#6B7280', 
                    fontSize: '14px',
                    fontWeight: '600',
                    marginBottom: '6px',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    合同总金额：
                  </div>
                  <div style={{ 
                    color: '#E31E24', 
                    fontSize: '14px',
                    fontWeight: '600',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    ¥{contractData.contract.totalAmount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} 元
                  </div>
                </div>
              </div>

              {/* 合同文件与摊销周期 */}
              <div style={{ 
                paddingTop: '16px', 
                borderTop: '1px solid #E5E5E5',
                display: 'flex',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '24px'
              }}>
                {/* 合同文件 */}
                <div>
                  <Text style={{ 
                    color: '#6B7280', 
                    fontSize: '14px',
                    fontWeight: '600',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    合同文件：
                  </Text>
                  <a 
                    href="/index.html" 
                    download={`${contractName}.pdf`}
                    style={{ 
                      color: '#4A90E2', 
                      fontWeight: '400',
                      textDecoration: 'none',
                      marginLeft: '4px',
                      fontSize: '14px',
                      fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                    }}
                    onMouseEnter={(e) => (e.target as HTMLElement).style.textDecoration = 'underline'}
                    onMouseLeave={(e) => (e.target as HTMLElement).style.textDecoration = 'none'}
                  >
                    {contractName}.pdf
                  </a>
                </div>

                {/* 摊销周期 */}
                <div>
                  <Text style={{ 
                    color: '#6B7280', 
                    fontSize: '14px',
                    fontWeight: '600',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    摊销周期：
                  </Text>
                  <Text style={{ 
                    color: '#E31E24', 
                    fontSize: '14px',
                    fontWeight: '400',
                    marginLeft: '4px',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    {calculateAmortizationPeriods()}
                  </Text>
                </div>
              </div>
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '40px 0' }}>
              <Text style={{ 
                color: '#999999',
                fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
              }}>
                暂无合同信息
              </Text>
            </div>
          )}
        </Spin>
      </div>

      {/* Tab页和数据表格 */}
      <div style={{
        backgroundColor: '#FFFFFF',
        borderRadius: '12px',
        padding: '4px',
        marginBottom: 24,
        border: '1px solid #E5E5E5'
      }}>
        <Tabs
          activeKey={activeKey}
          onChange={setActiveKey}
          items={[
            { 
              key: 'timeline', 
              label: <span style={{ 
                color: activeKey === 'timeline' ? '#E31E24' : '#6B7280', 
                fontWeight: activeKey === 'timeline' ? '600' : '500',
                fontSize: '14px'
              }}>预提支付</span>
            },
            { 
              key: 'accrual', 
              label: <span style={{ 
                color: activeKey === 'accrual' ? '#E31E24' : '#6B7280', 
                fontWeight: activeKey === 'accrual' ? '600' : '500',
                fontSize: '14px'
              }}>预提会计分录</span>
            },
            { 
              key: 'payment', 
              label: <span style={{ 
                color: activeKey === 'payment' ? '#E31E24' : '#6B7280', 
                fontWeight: activeKey === 'payment' ? '600' : '500',
                fontSize: '14px'
              }}>付款会计分录</span>
            },
          ]}
          style={{ marginBottom: 0 }}
        />
      </div>

      {/* 批量操作提示 */}
      {selectedRowKeys.length > 0 && activeKey === 'timeline' && (
        <div style={{ 
          marginBottom: 16, 
          padding: '12px 16px', 
          background: 'rgba(227, 30, 36, 0.05)', 
          border: '1px solid rgba(227, 30, 36, 0.2)', 
          borderRadius: '12px',
          borderLeft: '4px solid #E31E24'
        }}>
          <Text style={{ color: '#6B7280', fontSize: '14px' }}>
            <strong style={{ color: '#1F2937' }}>已选择 {selectedRowKeys.length} 项，可进行</strong>
            <Button 
              type="link" 
              size="small" 
              style={{ 
                padding: '0 8px', 
                height: 'auto', 
                color: '#E31E24', 
                fontWeight: '600',
                fontSize: '14px'
              }}
              onClick={handleBatchEdit}
            >
              批量操作
            </Button>
          </Text>
        </div>
      )}

      {activeKey === 'accrual' ? (
        renderAccrualRecords()
      ) : activeKey === 'payment' ? (
        renderPaymentRecords()
      ) : (
        <Spin 
          spinning={loading}
          className="outlook-spin"
        >
          <Table
            rowKey={(record) => record.id || Math.random()}
            columns={getColumnsByKey(activeKey)}
            dataSource={getDataSourceByKey(activeKey)}
            pagination={false}
            scroll={{ x: 1000 }}
            size="middle"
            rowSelection={activeKey === 'timeline' ? rowSelection : undefined}
          />
        </Spin>
      )}

      {/* 支付弹窗 */}
      <Modal
        title={
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <div style={{
              width: '4px',
              height: '20px',
              backgroundColor: '#E31E24',
              marginRight: '12px'
            }}></div>
            <span style={{ color: '#0F172A', fontSize: '18px', fontWeight: '600' }}>支付</span>
          </div>
        }
        open={isModalVisible}
        onOk={handleConfirmEdit}
        onCancel={handleCancelEdit}
        okText={<span style={{ fontWeight: '600' }}>确定</span>}
        cancelText={<span style={{ fontWeight: '600' }}>取消</span>}
        width={420}
        confirmLoading={paymentLoading}
        okButtonProps={{
          style: {
            backgroundColor: '#E31E24',
            borderColor: '#E31E24',
            color: '#FFFFFF',
            fontWeight: '600',
            borderRadius: '8px'
          }
        }}
        cancelButtonProps={{
          style: {
            borderColor: '#E5E5E5',
            color: '#6B7280',
            fontWeight: '600',
            borderRadius: '8px'
          }
        }}
      >
        <div style={{ padding: '20px 0' }}>
          <Space direction="vertical" style={{ width: '100%' }} size={16}>
            <Text style={{ color: '#6B7280', fontSize: '14px' }}>
              <strong style={{ color: '#1F2937' }}>期次：</strong>第 {currentEditRecord?.id} 期
            </Text>
            <Text style={{ color: '#6B7280', fontSize: '14px' }}>
              <strong style={{ color: '#1F2937' }}>当前金额：</strong>
              <span style={{ color: '#E31E24', fontWeight: '700', fontSize: '15px' }}>
                ¥{currentEditRecord?.amount.toFixed(2)}
              </span>
            </Text>
            <div>
              <Text style={{ 
                display: 'block', 
                marginBottom: '8px', 
                color: '#1F2937', 
                fontWeight: '600',
                fontSize: '14px'
              }}>
                支付金额：
              </Text>
              <InputNumber
                style={{ 
                  width: '100%',
                  borderRadius: '6px'
                }}
                value={newAmount}
                onChange={(value) => setNewAmount(value)}
                precision={2}
                min={0}
                max={999999999}
                prefix="¥"
                placeholder="请输入支付金额"
                size="large"
              />
            </div>
          </Space>
        </div>
      </Modal>

      {/* 批量支付弹窗 */}
      <Modal
        title="批量支付"
        open={isBatchModalVisible}
        onOk={handleConfirmBatchEdit}
        onCancel={handleCancelBatchEdit}
        okText="确定"
        cancelText="取消"
        width={600}
        confirmLoading={paymentLoading}
      >
        <div style={{ padding: '20px 0' }}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <Text>
              <strong>已选择 {selectedRowKeys.length} 项进行批量更改：</strong>
            </Text>
            
            {/* 显示选中的期次和金额信息 */}
            <div style={{ maxHeight: '200px', overflowY: 'auto', border: '1px solid #d9d9d9', borderRadius: '6px', padding: '12px' }}>
              {getSelectedRecords().map((record, index) => (
                <div key={record.id} style={{ 
                  display: 'flex', 
                  justifyContent: 'space-between', 
                  padding: '4px 0',
                  borderBottom: index < getSelectedRecords().length - 1 ? '1px solid #f0f0f0' : 'none'
                }}>
                  <Text>第 {record.id} 期 ({record.amortizationPeriod})</Text>
                  <Text>当前金额：¥{record.amount.toFixed(2)}</Text>
                </div>
              ))}
            </div>

            <div>
              <Text style={{ display: 'block', marginBottom: '8px' }}>
                <strong>批量支付金额：</strong>
              </Text>
              <InputNumber
                style={{ width: '100%' }}
                value={batchNewAmount}
                onChange={(value) => setBatchNewAmount(value)}
                precision={2}
                min={0}
                max={999999999}
                prefix="¥"
                placeholder="请输入批量支付金额（将应用到所有选中项）"
              />
            </div>
          </Space>
        </div>
      </Modal>
      </div>
    </>
  );
};

export default ContractDetail;
