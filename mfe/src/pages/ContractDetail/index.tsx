import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Typography, Table, Tabs, Spin, message, Button, Modal, InputNumber, Space, DatePicker } from 'antd';
import { DownloadOutlined, CalendarOutlined } from '@ant-design/icons';
import { getContractAmortizationEntries, ContractAmortizationResponse, ContractAmortizationEntry, executePayment, PaymentExecuteRequest, getContractPaymentRecords, updateContractStatus, getOperationLogsByContractId } from '../../api/contracts';
import { getJournalEntriesPreview, JournalEntriesPreviewResponse, DateRangeFilter, SortConfig } from '../../api/journalEntries';
import { JournalEntryImmutable } from './JournalEntryImmutable';
import dayjs from 'dayjs';

// 扩展类型以包含附件名称
type LocalContractAmortizationResponse = ContractAmortizationResponse & {
  contract?: {
    attachmentName: string;
  };
};

interface ContractAmortizationResponse {
  contract?: {
    id: string;
    vendorName: string;
    startDate: string;
    endDate: string;
    totalAmount: number;
    createdAt: string;
    attachmentName: string;
  };
  amortization?: ContractAmortizationEntry[];
}

const { Title, Text } = Typography;

const ContractDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [contractData, setContractData] = useState<LocalContractAmortizationResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [activeKey, setActiveKey] = useState('timeline');
  
  // 支付弹窗相关状态 
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [currentEditRecord, setCurrentEditRecord] = useState<ContractAmortizationEntry | null>(null);
  const [newAmount, setNewAmount] = useState<number | null>(null);
  const [paymentDate, setPaymentDate] = useState<dayjs.Dayjs | null>(null);
  
  // 批量编辑弹窗相关状态
  const [isBatchModalVisible, setIsBatchModalVisible] = useState(false);
  const [batchNewAmount, setBatchNewAmount] = useState<number | null>(null);
  const [batchPaymentDate, setBatchPaymentDate] = useState<dayjs.Dayjs | null>(null);
  
  // 多选相关状态
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  
  // 支付加载状态
  const [paymentLoading, setPaymentLoading] = useState(false);
  
  
  // 预提会计分录预览数据
  const [journalEntriesData, setJournalEntriesData] = useState<JournalEntriesPreviewResponse | null>(null);
  const [journalEntriesLoading, setJournalEntriesLoading] = useState(false);
  
  // 付款会计分录数据（实际执行后的分录）
  const [paymentJournalEntriesData, setPaymentJournalEntriesData] = useState<any[]>([]);
  const [paymentJournalEntriesLoading, setPaymentJournalEntriesLoading] = useState(false);
  
  // 预提会计分录筛选和排序状态
  const [dateRangeFilter, setDateRangeFilter] = useState<DateRangeFilter>({});
  const [sortConfig, setSortConfig] = useState<SortConfig>({ field: 'entryOrder', order: 'asc' });
  
  // 是否为初始加载
  const [isInitialLoad, setIsInitialLoad] = useState(true);

  // 导出月份选择状态
  const [selectedExportMonth, setSelectedExportMonth] = useState<dayjs.Dayjs | null>(null);

  // 操作记录相关状态
  const [operationLogs, setOperationLogs] = useState<any[]>([]);
  const [operationLogsLoading, setOperationLogsLoading] = useState(false);
  const hasRecordedContractGenerationRef = useRef(false);


  // 从路由参数获取contractId
  const contractId = id ? parseInt(id, 10) : null;

  // 验证contractId
  useEffect(() => {
    if (!contractId || isNaN(contractId)) {
      message.error('无效的合同ID');
      navigate('/page-a');
    }
  }, [contractId, navigate]);

  // 获取合同摊销明细数据
  const fetchContractData = useCallback(async (shouldRecordOperation = false) => {
    if (!contractId) return;
    
    setLoading(true);
    try {
      const response = await getContractAmortizationEntries(contractId);
      setContractData(response);
      
      // 记录合同生成操作（仅在初始加载时记录）
      if (shouldRecordOperation && !hasRecordedContractGenerationRef.current) {
        const contractCreatedAt = response.contract?.createdAt;
        addOperationLog(
          '生成', 
          `合同摊销明细生成完成，供应商：${response.contract?.vendorName || '未知'}`,
          contractCreatedAt
        );
        hasRecordedContractGenerationRef.current = true;
      }
    } catch (error) {
      message.error('获取合同摊销明细失败');
      console.error('API调用失败:', error);
    } finally {
      setLoading(false);
    }
  }, [contractId]);

  // 检查所有摊销条目是否已完成，并更新合同状态
  const checkAndUpdateContractStatus = useCallback(async (amortizationEntries: ContractAmortizationEntry[]) => {
    if (!contractId || !amortizationEntries || amortizationEntries.length === 0) return;
    
    // 检查是否所有摊销条目都已完成付款
    const allCompleted = amortizationEntries.every(entry => 
      entry.paymentStatus === 'COMPLETED' || entry.paymentStatus === 'PAID'
    );
    
    if (allCompleted) {
      try {
        const response = await updateContractStatus(contractId, 'COMPLETED');
        console.log('合同状态已更新:', response.message);
        
        // 记录合同完成操作
        addOperationLog('完成', '所有摊销条目已付款完成，合同状态已更新为已完成');
      } catch (error) {
        console.error('更新合同状态失败:', error);
      }
    }
  }, [contractId]);

  // 获取预提会计分录预览数据
  const fetchJournalEntriesPreview = async () => {
    if (!contractId) return;
    
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

  // 获取付款会计分录数据（实际执行后的分录）
  const fetchPaymentJournalEntries = async () => {
    if (!contractId) return;
    
    setPaymentJournalEntriesLoading(true);
    try {
      // 使用API客户端调用后端获取合同的实际付款记录及其会计分录
      const payments = await getContractPaymentRecords(contractId);
      
      // 提取所有付款的会计分录
      const allEntries: any[] = [];
      if (payments && Array.isArray(payments)) {
        payments.forEach((payment: any, paymentIndex: number) => {
          if (payment.journalEntries) {
            payment.journalEntries.forEach((entry: any, entryIndex: number) => {
              allEntries.push({
                key: `${payment.paymentId}-${entryIndex}`,
                paymentId: payment.paymentId,
                paymentDate: payment.bookingDate,
                entryOrder: entry.entryOrder || (entryIndex + 1),
                entryType: entry.entryType || 'PAYMENT',
                bookingDate: entry.bookingDate,
                account: entry.account,
                debitAmount: entry.dr || entry.debitAmount || 0,
                creditAmount: entry.cr || entry.creditAmount || 0,
                memo: entry.memo || entry.description || '',
                paymentTimestamp: entry.paymentTimestamp || entry.accountingReviewTime,
                paymentAmount: payment.paymentAmount,
                amortizationPeriod: entry.amortizationPeriod || ''
              });
            });
          }
        });
      }
      setPaymentJournalEntriesData(allEntries);
    } catch (error) {
      console.error('获取付款会计分录失败:', error);
      setPaymentJournalEntriesData([]);
    } finally {
      setPaymentJournalEntriesLoading(false);
    }
  };

  // 导出付款分录和预提摊销分录为Excel
  const exportPaymentEntriesToExcel = () => {
    // 调试：查看预提分录数据结构
    console.log('预提分录原始数据:', journalEntriesData?.previewEntries);
    if (journalEntriesData?.previewEntries && journalEntriesData.previewEntries.length > 0) {
      console.log('第一条预提分录示例:', journalEntriesData.previewEntries[0]);
    }
    
    // 准备Excel数据
    const headers = [
      '分录类型',
      '付款ID',
      '入账日期', 
      '科目名称',
      '借方金额',
      '贷方金额',
      '分录描述',
      '摊销期间',
      '会计复核时间'
    ];

    // 获取付款分录数据
    let filteredPaymentEntries = getFilteredAndSortedPaymentEntries();
    
    // 获取预提摊销分录数据
    let filteredAccrualEntries = journalEntriesData?.previewEntries || [];
    
    // 如果选择了特定月份，则过滤该月份的数据
    if (selectedExportMonth) {
      const selectedMonth = selectedExportMonth.format('YYYY-MM');
      
      // 过滤付款分录
      filteredPaymentEntries = filteredPaymentEntries.filter(entry => {
        if (entry.bookingDate) {
          const entryMonth = dayjs(entry.bookingDate).format('YYYY-MM');
          return entryMonth === selectedMonth;
        }
        return false;
      });
      
      // 过滤预提摊销分录
      filteredAccrualEntries = filteredAccrualEntries.filter(entry => {
        if (entry.bookingDate) {
          const entryMonth = dayjs(entry.bookingDate).format('YYYY-MM');
          return entryMonth === selectedMonth;
        }
        return false;
      });
      
      if (filteredPaymentEntries.length === 0 && filteredAccrualEntries.length === 0) {
        message.warning(`${selectedMonth} 月份暂无分录数据`);
        return;
      }
    } else {
      // 如果没有选择月份，检查是否有数据
      if (filteredPaymentEntries.length === 0 && filteredAccrualEntries.length === 0) {
        message.warning('暂无数据可导出');
        return;
      }
    }
    
    // 合并付款分录和预提摊销分录
    const allEntries: any[] = [];
    
    // 添加付款分录
    filteredPaymentEntries.forEach(entry => {
      allEntries.push({
        type: '付款分录',
        paymentId: entry.paymentId || '',
        bookingDate: entry.bookingDate || '',
        account: entry.account || '',
        debitAmount: entry.debitAmount || '0.00',
        creditAmount: entry.creditAmount || '0.00',
        memo: entry.memo || '',
        amortizationPeriod: entry.amortizationPeriod || '',
        reviewTime: entry.paymentTimestamp ? new Date(entry.paymentTimestamp).toLocaleString('zh-CN') : '',
        sortDate: entry.bookingDate ? new Date(entry.bookingDate).getTime() : 0
      });
    });
    
    // 添加预提摊销分录（使用已过滤的数据）
    filteredAccrualEntries.forEach(entry => {
        allEntries.push({
          type: '预提摊销分录',
          paymentId: '',
          bookingDate: entry.bookingDate || '',
          account: entry.accountName || entry.account || '',  // 使用accountName字段
          debitAmount: entry.debitAmount || '0.00',
          creditAmount: entry.creditAmount || '0.00',
          memo: entry.memo || '',
          amortizationPeriod: entry.amortizationPeriod || entry.accountingPeriod || '',  // 尝试多个字段
          reviewTime: '',
          sortDate: entry.bookingDate ? new Date(entry.bookingDate).getTime() : 0
        });
    });
    
    // 按月份分组
    const groupedByMonth: { [key: string]: any[] } = {};
    allEntries.forEach(entry => {
      const month = entry.bookingDate ? dayjs(entry.bookingDate).format('YYYY-MM') : '未知';
      if (!groupedByMonth[month]) {
        groupedByMonth[month] = [];
      }
      groupedByMonth[month].push(entry);
    });
    
    // 按月份排序（降序）
    const sortedMonths = Object.keys(groupedByMonth).sort((a, b) => b.localeCompare(a));
    
    // 在每个月份内按会计科目排序
    sortedMonths.forEach(month => {
      groupedByMonth[month].sort((a, b) => {
        // 首先按会计科目排序
        const accountCompare = a.account.localeCompare(b.account, 'zh-CN');
        if (accountCompare !== 0) return accountCompare;
        
        // 相同科目按入账日期排序
        return a.sortDate - b.sortDate;
      });
    });

    // 创建工作表数据
    const worksheetData = [];
    
    // 添加标题行
    worksheetData.push(headers);
    
    // 按月份添加数据
    sortedMonths.forEach(month => {
      const entries = groupedByMonth[month];
      
      // 添加月份标题
      worksheetData.push([
        `${month} 月份分录`,
        '',
        '',
        '',
        '',
        '',
        '',
        '',
        ''
      ]);
      
      // 添加该月份的所有分录（已按科目排序）
      entries.forEach((entry: any) => {
        worksheetData.push([
          entry.type,
          entry.paymentId,
          entry.bookingDate,
          entry.account,
          entry.debitAmount,
          entry.creditAmount,
          entry.memo,
          entry.amortizationPeriod,
          entry.reviewTime
        ]);
      });
      
      // 添加空行分隔
      worksheetData.push(['', '', '', '', '', '', '', '', '']);
    });

    // 创建简单的CSV格式（兼容Excel）
    const csvContent = worksheetData
      .map(row => row.map(cell => `"${cell}"`).join(','))
      .join('\n');

    // 添加BOM以支持中文
    const BOM = '\uFEFF';
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' });
    
    // 创建下载链接
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    const monthSuffix = selectedExportMonth ? `_${selectedExportMonth.format('YYYY-MM')}月` : '';
    link.setAttribute('download', `会计分录_合同${contractId}${monthSuffix}_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    // 计算各类型分录数量
    const paymentCount = allEntries.filter(e => e.type === '付款分录').length;
    const accrualCount = allEntries.filter(e => e.type === '预提摊销分录').length;
    const exportedCount = `（付款分录: ${paymentCount}条，预提摊销分录: ${accrualCount}条）`;
    message.success(`导出成功 ${exportedCount}`);
    
    // 记录导出操作
    addOperationLog('导出', `导出会计分录${monthSuffix} ${exportedCount}`);
  };

  // 添加操作记录（后端已自动记录，前端只需刷新列表）
  const addOperationLog = async (operationType: string, description: string, customTimestamp?: string) => {
    if (!contractId) return;
    
    // 后端已经在各个操作中自动记录了操作日志
    // 前端只需要刷新操作日志列表即可
    console.log('操作记录已由后端自动生成:', operationType, description);
    
    // 延迟一下再刷新，确保后端已经保存
    setTimeout(async () => {
      await fetchOperationLogs();
    }, 500);
  };

  // 获取操作记录数据
  const fetchOperationLogs = async () => {
    if (!contractId) return;
    
    setOperationLogsLoading(true);
    try {
      const response = await getOperationLogsByContractId(contractId);
      
      if (response.success) {
        // 将后端返回的 operationTime 映射为前端使用的 timestamp
        const mappedLogs = response.data.map((log: any) => ({
          ...log,
          timestamp: log.operationTime || log.createdAt
        }));
        setOperationLogs(mappedLogs);
      } else {
        // 如果API失败，从localStorage加载
        const storageKey = `operationLogs_${contractId}`;
        const storedLogs = localStorage.getItem(storageKey);
        
        if (storedLogs) {
          const logs = JSON.parse(storedLogs);
          setOperationLogs(logs);
        } else {
          setOperationLogs([]);
        }
      }
      
      setOperationLogsLoading(false);
    } catch (error) {
      console.error('获取操作记录失败:', error);
      
      // API失败时从localStorage加载
      const storageKey = `operationLogs_${contractId}`;
      const storedLogs = localStorage.getItem(storageKey);
      
      if (storedLogs) {
        const logs = JSON.parse(storedLogs);
        setOperationLogs(logs);
      } else {
        setOperationLogs([]);
      }
      
      setOperationLogsLoading(false);
    }
  };

  // 处理付款分录数据的排序和筛选
  const getFilteredAndSortedPaymentEntries = () => {
    if (!paymentJournalEntriesData || paymentJournalEntriesData.length === 0) return [];
    
    let filteredEntries = [...paymentJournalEntriesData];
    
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

  // 计算摊销周期
  const calculateAmortizationPeriods = () => {
    if (!contractData?.contract) return '';
    const startDate = new Date(contractData.contract.startDate);
    const endDate = new Date(contractData.contract.endDate);
    const monthDiff = (endDate.getFullYear() - startDate.getFullYear()) * 12 + 
                      (endDate.getMonth() - startDate.getMonth()) + 1;
    return `共 ${monthDiff} 期，按月摊销`;
  };

  // 组件加载时获取数据
  useEffect(() => {
    fetchContractData(true); // 初始加载时记录操作
    setIsInitialLoad(false); // 初始加载完成
  }, [fetchContractData]);

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
        // 付款会计分录页面，获取实际的付款分录
        await fetchPaymentJournalEntries();
      } else if (activeKey === 'operations') {
        // 操作记录页面，获取操作记录
        await fetchOperationLogs();
      }
    };

    // 执行Tab切换加载逻辑
    handleTabDataLoading();
  }, [activeKey, isInitialLoad]);

  // 打开支付弹窗
  const handleEditAmount = (record: ContractAmortizationEntry) => {
    // 检查是否已完成，如果已完成则不允许再次付款
    if (record.paymentStatus === 'COMPLETED' || record.paymentStatus === 'PAID') {
      message.warning('该期间已完成付款，无法重复操作');
      return;
    }
    
    setCurrentEditRecord(record);
    setNewAmount(record.amount);
    setPaymentDate(dayjs()); // 默认为当天
    setIsModalVisible(true);
  };

  // 确认支付
  const handleConfirmEdit = async () => {
    if (!contractId) return;
    
    if (!currentEditRecord || newAmount === null) {
      message.warning('请输入有效金额');
      return;
    }

    if (!paymentDate) {
      message.warning('请选择支付时间');
      return;
    }

    // 重新获取最新数据，检查当前记录是否已被其他操作完成付款
    await fetchContractData();
    
    // 从最新数据中查找当前记录
    const latestRecord = contractData?.amortization?.find(record => record.id === currentEditRecord.id);
    if (!latestRecord) {
      message.error('记录不存在，请刷新页面重试');
      setIsModalVisible(false);
      return;
    }
    
    // 检查记录是否已完成付款
    if (latestRecord.paymentStatus === 'COMPLETED' || latestRecord.paymentStatus === 'PAID') {
      message.warning('该期间已完成付款，无法重复操作');
      setIsModalVisible(false);
      setCurrentEditRecord(null);
      setNewAmount(null);
      return;
    }

    setPaymentLoading(true);
    
    // 显示处理中的提示
    const hideLoading = message.loading('正在处理付款...', 0);
    
    try {
      // 获取当前登录用户
      const currentUser = localStorage.getItem('username') || 'system';
      
      // 构造支付请求参数
      const paymentRequest: PaymentExecuteRequest = {
        contractId,
        paymentAmount: newAmount,
        selectedPeriods: [currentEditRecord.id], // 选中的期次
        paymentDate: paymentDate.format('YYYY-MM-DD HH:mm:ss'), // 支付时间
        operatorId: currentUser // 操作人ID
      };

      // 调用支付接口
      const response = await executePayment(paymentRequest);
      
      // 隐藏处理中提示
      hideLoading();
      
      // 显示支付结果
      message.success(response.message || '付款执行成功');
      
      // 关闭弹窗并重置状态
      setIsModalVisible(false);
      setCurrentEditRecord(null);
      setNewAmount(null);
      setPaymentDate(null);
      
      // 添加短暂延迟确保后端数据已更新
      await new Promise(resolve => setTimeout(resolve, 300));
      
      // 重新获取合同数据以更新摊销条目状态
      await fetchContractData();
      
      // 重新获取操作记录（后端已自动创建付款记录）
      await fetchOperationLogs();
      
      // 等待状态更新后再检查合同状态
      setTimeout(async () => {
        if (contractData?.amortization) {
          await checkAndUpdateContractStatus(contractData.amortization);
        }
      }, 100);
      
      // 清理已完成记录的选中状态
      setSelectedRowKeys(prev => {
        if (!contractData?.amortization) return [];
        return prev.filter(key => {
          const record = contractData.amortization.find(r => r.id === key);
          return record && record.paymentStatus !== 'COMPLETED' && record.paymentStatus !== 'PAID';
        });
      });
      
      // 额外的状态更新来触发重新渲染
      setLoading(true);
      await new Promise(resolve => setTimeout(resolve, 50));
      setLoading(false);
      
      // 无论当前在哪个页面，都刷新付款分录数据以保持数据一致性
      await fetchPaymentJournalEntries();
      
      // 如果当前在预提会计分录页面，也刷新预提分录数据
      if (activeKey === 'accrual') {
        await fetchJournalEntriesPreview();
      }
      
      // 付款完成后取消所有选中的复选框并切换到预提支付页面
      setSelectedRowKeys([]);
      setActiveKey('timeline');
    } catch (error) {
      // 隐藏处理中提示
      hideLoading();
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
    
    // 检查选中的记录中是否有已完成的记录
    const selectedRecords = getSelectedRecords();
    const completedRecords = selectedRecords.filter(record => 
      record.paymentStatus === 'COMPLETED' || record.paymentStatus === 'PAID'
    );
    
    if (completedRecords.length > 0) {
      message.warning('选中的记录中包含已完成的付款，请重新选择');
      return;
    }
    
    setBatchNewAmount(null);
    setBatchPaymentDate(dayjs()); // 默认为当天
    setIsBatchModalVisible(true);
  };

  // 确认批量支付
  const handleConfirmBatchEdit = async () => {
    if (!contractId) return;
    
    if (batchNewAmount === null) {
      message.warning('请输入有效金额');
      return;
    }

    if (!batchPaymentDate) {
      message.warning('请选择支付时间');
      return;
    }

    // 重新获取最新数据，检查选中的记录是否有已完成付款的
    await fetchContractData();
    
    // 获取最新的选中记录
    const latestSelectedRecords = contractData?.amortization?.filter(record => 
      selectedRowKeys.includes(record.id)
    ) || [];
    
    // 过滤掉已完成的记录
    const validRecords = latestSelectedRecords.filter(record => 
      record.paymentStatus !== 'COMPLETED' && record.paymentStatus !== 'PAID'
    );
    
    // 检查是否还有有效记录
    if (validRecords.length === 0) {
      message.warning('所选记录均已完成付款，无法执行批量操作');
      setIsBatchModalVisible(false);
      setBatchNewAmount(null);
      setSelectedRowKeys([]);
      return;
    }
    
    // 如果有部分记录已完成，提示用户
    if (validRecords.length < latestSelectedRecords.length) {
      const completedCount = latestSelectedRecords.length - validRecords.length;
      message.info(`已过滤 ${completedCount} 条已完成的记录，将对剩余 ${validRecords.length} 条记录执行付款`);
    }

    setPaymentLoading(true);
    
    // 显示处理中的提示
    const hideLoading = message.loading('正在处理批量付款...', 0);
    
    try {
      // 获取当前登录用户
      const currentUser = localStorage.getItem('username') || 'system';
      
      // 构造批量支付请求参数，只包含有效记录
      const paymentRequest: PaymentExecuteRequest = {
        contractId,
        paymentAmount: batchNewAmount,
        selectedPeriods: validRecords.map(record => record.id), // 只提交有效的期次
        paymentDate: batchPaymentDate.format('YYYY-MM-DD HH:mm:ss'), // 支付时间
        operatorId: currentUser // 操作人ID
      };

      // 调用支付接口
      const response = await executePayment(paymentRequest);
      
      // 隐藏处理中提示
      hideLoading();
      
      // 显示支付结果
      message.success(response.message || '批量付款执行成功');
      
      // 关闭弹窗并重置状态
      setIsBatchModalVisible(false);
      setBatchNewAmount(null);
      setBatchPaymentDate(null);
      setSelectedRowKeys([]); // 清空选择
      
      // 添加短暂延迟确保后端数据已更新
      await new Promise(resolve => setTimeout(resolve, 300));
      
      // 重新获取合同数据以更新摊销条目状态
      await fetchContractData();
      
      // 重新获取操作记录（后端已自动创建付款记录）
      await fetchOperationLogs();
      
      // 等待状态更新后再检查合同状态
      setTimeout(async () => {
        if (contractData?.amortization) {
          await checkAndUpdateContractStatus(contractData.amortization);
        }
      }, 100);
      
      // 强制重新渲染以确保按钮样式更新
      await new Promise(resolve => setTimeout(resolve, 50));
      
      // 额外的状态更新来触发重新渲染
      setLoading(true);
      await new Promise(resolve => setTimeout(resolve, 50));
      setLoading(false);
      
      // 无论当前在哪个页面，都刷新付款分录数据以保持数据一致性
      await fetchPaymentJournalEntries();
      
      // 如果当前在预提会计分录页面，也刷新预提分录数据
      if (activeKey === 'accrual') {
        await fetchJournalEntriesPreview();
      }
    } catch (error) {
      // 隐藏处理中提示
      hideLoading();
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
    setBatchPaymentDate(null);
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
    getCheckboxProps: (record: ContractAmortizationEntry) => ({
      disabled: record.paymentStatus === 'COMPLETED' || record.paymentStatus === 'PAID',
      name: record.amortizationPeriod,
    }),
  };

  // 预提支付表表头（匹配API返回数据）
  const columns = [
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>序号</span>, 
      key: 'index', 
      width: 80,
      align: 'center' as const,
      render: (_: any, __: any, index: number) => (
        <span style={{ color: '#0F172A', fontWeight: '500', fontSize: '14px' }}>
          {index + 1}
        </span>
      )
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
          'PAID': { text: '已完成', color: '#065F46', bgColor: '#D1FAE5' },
          'COMPLETED': { text: '已完成', color: '#065F46', bgColor: '#D1FAE5' },
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
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>支付时间</span>, 
      dataIndex: 'paymentDate', 
      key: 'paymentDate', 
      width: 180,
      render: (date: string) => (
        <span style={{ color: '#6B7280', fontSize: '13px' }}>
          {date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'}
        </span>
      )
    },
    { 
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>操作</span>, 
      key: 'action', 
      width: 180, 
      render: (_: any, record: ContractAmortizationEntry) => {
        const isCompleted = record.paymentStatus === 'COMPLETED' || record.paymentStatus === 'PAID';
        return (
          <Space size="small">
            <Button 
              key={`${record.id}-${record.paymentStatus}-${Date.now()}`}
              type={isCompleted ? "default" : "primary"}
              size="small" 
              disabled={isCompleted}
              onClick={() => handleEditAmount(record)}
              style={{
                backgroundColor: isCompleted ? '#F3F4F6' : '#E31E24',
                borderColor: isCompleted ? '#D1D5DB' : '#E31E24',
                color: isCompleted ? '#9CA3AF' : '#FFFFFF',
                fontWeight: '600',
                fontSize: '13px',
                borderRadius: '8px',
                cursor: isCompleted ? 'not-allowed' : 'pointer'
              }}
              onMouseEnter={(e) => {
                if (!isCompleted) {
                  (e.target as HTMLElement).style.backgroundColor = '#C41E3A';
                  (e.target as HTMLElement).style.borderColor = '#C41E3A';
                } else {
                  // 已完成状态保持灰色，不改变颜色
                  (e.target as HTMLElement).style.backgroundColor = '#F3F4F6';
                  (e.target as HTMLElement).style.borderColor = '#D1D5DB';
                }
              }}
              onMouseLeave={(e) => {
                if (!isCompleted) {
                  (e.target as HTMLElement).style.backgroundColor = '#E31E24';
                  (e.target as HTMLElement).style.borderColor = '#E31E24';
                } else {
                  // 已完成状态保持灰色，不改变颜色
                  (e.target as HTMLElement).style.backgroundColor = '#F3F4F6';
                  (e.target as HTMLElement).style.borderColor = '#D1D5DB';
                }
              }}
            >
              {isCompleted ? '已完成' : '支付'}
            </Button>
          </Space>
        );
      }
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
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>分录描述
      </span>, 
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
    { title: '分录描述', dataIndex: 'remark', key: 'remark' },
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
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>分录描述
      </span>,
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
      dataIndex: 'account',
      key: 'account',
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
      dataIndex: 'memo',
      key: 'memo',
      width: 200,
      render: (memo: string) => (
        <span style={{ color: '#6B7280', fontSize: '13px' }}>{memo}</span>
      )
    },
    {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>会计复核时间</span>,
      dataIndex: 'paymentTimestamp',
      key: 'paymentTimestamp',
      width: 180,
      render: (date: string) => (
        <span style={{ color: '#1F2937', fontSize: '13px' }}>
          {date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'}
        </span>
      )
    }
  ];

  // 渲染付款会计分录页面 - 按付款ID分组展示
  const renderPaymentRecords = () => {
    if (paymentJournalEntriesLoading) {
      return (
        <div style={{ textAlign: "center", padding: "40px 0" }}>
          <Spin size="large" className="outlook-spin" />
          <div style={{ marginTop: 16 }}>
            <Text style={{ color: "#6B7280", fontSize: "14px" }}>正在加载付款会计分录数据...</Text>
          </div>
        </div>
      );
    }

    if (!paymentJournalEntriesData || paymentJournalEntriesData.length === 0) {
      return (
        <div style={{ textAlign: "center", padding: "40px 0" }}>
          <Text type="secondary">暂无付款会计分录数据，请先执行付款操作</Text>
        </div>
      );
    }

    const filteredEntries = getFilteredAndSortedPaymentEntries();
    
    // 按付款ID分组
    const groupedByPayment: { [key: string]: any[] } = {};
    filteredEntries.forEach(entry => {
      const paymentKey = entry.paymentId ? `payment-${entry.paymentId}` : 'no-payment';
      if (!groupedByPayment[paymentKey]) {
        groupedByPayment[paymentKey] = [];
      }
      groupedByPayment[paymentKey].push(entry);
    });
    
    // 按付款ID排序（数字降序，最新的付款在前）
    const sortedPaymentKeys = Object.keys(groupedByPayment).sort((a, b) => {
      const aId = a.startsWith('payment-') ? parseInt(a.split('-')[1]) : 0;
      const bId = b.startsWith('payment-') ? parseInt(b.split('-')[1]) : 0;
      return bId - aId;
    });
    
    // 在每个付款组内按入账日期排序
    sortedPaymentKeys.forEach(paymentKey => {
      groupedByPayment[paymentKey].sort((a, b) => {
        const dateA = a.bookingDate ? new Date(a.bookingDate).getTime() : 0;
        const dateB = b.bookingDate ? new Date(b.bookingDate).getTime() : 0;
        return dateA - dateB; // 升序，早的在前
      });
    });

    return (
      <div>
        {/* 导出控制区域 */}
        <div style={{ 
          marginBottom: 16, 
          display: 'flex', 
          justifyContent: 'flex-end',
          alignItems: 'center',
          gap: '12px'
        }}>
          <Space>
            <DatePicker
              picker="month"
              placeholder="选择月份（可选）"
              value={selectedExportMonth}
              onChange={setSelectedExportMonth}
              allowClear
              style={{ width: 160 }}
              suffixIcon={<CalendarOutlined style={{ color: '#6B7280' }} />}
            />
            <Button 
              type="primary" 
              icon={<DownloadOutlined />}
              onClick={exportPaymentEntriesToExcel}
              style={{
                backgroundColor: '#E31E24',
                borderColor: '#E31E24',
                borderRadius: '6px',
                fontWeight: '600'
              }}
            >
              {selectedExportMonth ? `导出${selectedExportMonth.format('YYYY年MM月')}分录` : '导出全部分录'}
            </Button>
          </Space>
        </div>
        
        {sortedPaymentKeys.map((paymentKey, index) => {
          const paymentId = paymentKey.startsWith('payment-') ? paymentKey.split('-')[1] : '未知';
          const entries = groupedByPayment[paymentKey];
          const paymentDate = entries[0]?.paymentTimestamp ? 
            new Date(entries[0].paymentTimestamp).toLocaleDateString('zh-CN') : 
            new Date(entries[0]?.bookingDate).toLocaleDateString('zh-CN');
          
          return (
            <div key={paymentKey} style={{ marginBottom: 24 }}>
              {/* 付款标题 */}
              <div style={{
                backgroundColor: "#F8F9FA",
                padding: "12px 16px",
                borderRadius: "8px 8px 0 0",
                borderBottom: "2px solid #E31E24",
                marginBottom: 0
              }}>
                <Text style={{
                  fontSize: "16px",
                  fontWeight: "600",
                  color: "#1F2937"
                }}>
                  付款ID：{paymentId} - {paymentDate}
                </Text>
                <Text style={{
                  marginLeft: 16,
                  fontSize: "14px",
                  color: "#6B7280"
                }}>
                  ({entries.length} 条分录)
                </Text>
              </div>
              
              {/* 付款分录表格 */}
              <div style={{
                backgroundColor: "#FFFFFF",
                borderRadius: index === sortedPaymentKeys.length - 1 ? "0 0 12px 12px" : "0",
                border: "1px solid #E5E5E5",
                borderTop: "none",
                overflow: "hidden"
              }}>
                <Table
                  columns={paymentEntriesColumns}
                  dataSource={entries}
                  pagination={false}
                  size="middle"
                  rowKey="entryOrder"
                  scroll={{ x: 1000 }}
                />
              </div>
            </div>
          );
        })}
        
        {/* 总计信息 */}
        <div style={{
          backgroundColor: "#F8F9FA",
          padding: "16px",
          borderRadius: "8px",
          marginTop: 16
        }}>
          <Text style={{ fontSize: "14px", fontWeight: "600", color: "#1F2937" }}>
            总计：{sortedPaymentKeys.length} 笔付款，共 {filteredEntries.length} 条分录
          </Text>
        </div>
        
        {/* 借贷平衡总计行 */}
        {JournalEntryImmutable.renderTotalRow(filteredEntries)}
      </div>
    );
  };

  // 渲染操作记录页面
  const renderOperationLogs = () => {
    const operationColumns = [
      {
        title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>操作时间</span>,
        dataIndex: 'timestamp',
        key: 'timestamp',
        width: 180,
        render: (timestamp: string) => (
          <span style={{ color: '#1F2937', fontSize: '13px' }}>
            {dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')}
          </span>
        )
      },
      {
        title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>操作类型</span>,
        dataIndex: 'operationType',
        key: 'operationType',
        width: 120,
        render: (type: string) => {
          const colors = {
            '生成': '#52C41A',
            '付款': '#1890FF', 
            '导出': '#722ED1'
          };
          return (
            <span style={{ 
              color: colors[type as keyof typeof colors] || '#666',
              fontSize: '13px',
              fontWeight: '500'
            }}>
              {type}
            </span>
          );
        }
      },
      {
        title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>操作描述</span>,
        dataIndex: 'description',
        key: 'description',
        render: (description: string) => (
          <span style={{ color: '#1F2937', fontSize: '13px' }}>{description}</span>
        )
      },
      {
        title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>操作人</span>,
        dataIndex: 'operator',
        key: 'operator',
        width: 120,
        render: (operator: string) => (
          <span style={{ color: '#6B7280', fontSize: '13px' }}>{operator}</span>
        )
      }
    ];

    if (operationLogsLoading) {
      return (
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Spin size="large" className="outlook-spin" />
          <div style={{ marginTop: 16 }}>
            <Text style={{ color: '#6B7280', fontSize: '14px' }}>正在加载操作记录...</Text>
          </div>
        </div>
      );
    }

    return (
      <div>
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          border: '1px solid #E5E5E5',
          overflow: 'hidden'
        }}>
          <Table
            columns={operationColumns}
            dataSource={operationLogs}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条记录`
            }}
            size="middle"
            rowKey="id"
            scroll={{ x: 800 }}
            locale={{
              emptyText: (
                <div style={{ padding: '40px 0' }}>
                  <Text type="secondary">暂无操作记录</Text>
                </div>
              )
            }}
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

                {/* 动态渲染自定义字段 */}
                {contractData.contract.customFields && Object.entries(contractData.contract.customFields).map(([key, value]) => (
                  <div key={key}>
                    <div style={{ 
                      color: '#6B7280', 
                      fontSize: '14px',
                      fontWeight: '600',
                      marginBottom: '6px',
                      fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                    }}>
                      {key}：
                    </div>
                    <div style={{ 
                      color: '#1F2937', 
                      fontSize: '14px',
                      fontWeight: '400',
                      fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                    }}>
                      {value}
                    </div>
                  </div>
                ))}

                {/* 创建时间 */}
                <div>
                  <div style={{ 
                    color: '#6B7280', 
                    fontSize: '14px',
                    fontWeight: '600',
                    marginBottom: '6px',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    创建时间：
                  </div>
                  <div style={{ 
                    color: '#1F2937', 
                    fontSize: '14px',
                    fontWeight: '400',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    {contractData.amortization?.[0]?.createdAt ? dayjs(contractData.amortization[0].createdAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
                  </div>
                </div>

                {/* 摊销周期 */}
                <div>
                  <div style={{ 
                    color: '#6B7280', 
                    fontSize: '14px',
                    fontWeight: '600',
                    marginBottom: '6px',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    摊销周期：
                  </div>
                  <div style={{ 
                    color: '#1F2937', 
                    fontSize: '14px',
                    fontWeight: '400',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    {calculateAmortizationPeriods()}
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

                {/* 合同附件名称 */}
                <div>
                  <div style={{ 
                    color: '#6B7280', 
                    fontSize: '12px',
                    fontWeight: '500',
                    marginBottom: '4px'
                  }}>
                    合同附件名称
                  </div>
                  <div style={{ 
                    color: '#0F172A', 
                    fontSize: '14px',
                    fontWeight: '400',
                    fontFamily: 'Microsoft YaHei, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'
                  }}>
                    {contractData?.contract?.['attachmentName'] || '无'}
                  </div>
                </div>
              </div>

              {/* 合同文件 */}
              <div style={{ 
                paddingTop: '16px', 
                borderTop: '1px solid #E5E5E5',
                display: 'flex',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '24px'
              }}>
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
        padding: '4px',
        marginBottom: 24
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
            { 
              key: 'operations', 
              label: <span style={{ 
                color: activeKey === 'operations' ? '#E31E24' : '#6B7280', 
                fontWeight: activeKey === 'operations' ? '600' : '500',
                fontSize: '14px'
              }}>操作记录</span>
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
      ) : activeKey === 'operations' ? (
        renderOperationLogs()
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
            <div>
              <Text style={{ 
                display: 'block', 
                marginBottom: '8px', 
                color: '#1F2937', 
                fontWeight: '600',
                fontSize: '14px'
              }}>
                支付时间：
              </Text>
              <DatePicker
                style={{ 
                  width: '100%',
                  borderRadius: '6px'
                }}
                value={paymentDate}
                onChange={(date) => setPaymentDate(date)}
                showTime
                format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择支付时间"
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
            <div>
              <Text style={{ display: 'block', marginBottom: '8px' }}>
                <strong>支付时间：</strong>
              </Text>
              <DatePicker
                style={{ width: '100%' }}
                value={batchPaymentDate}
                onChange={(date) => setBatchPaymentDate(date)}
                showTime
                format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择支付时间"
                size="large"
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
