import React, { useState, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Upload, 
  Table, 
  Button, 
  Space, 
  message, 
  Spin,
  Typography,
  Checkbox,
  Input,
  Tag
} from 'antd';
import { 
  InboxOutlined, 
  EyeOutlined, 
  DeleteOutlined,
  FileTextOutlined
} from '@ant-design/icons';
import type { UploadProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import { PageAProps, ContractData } from './types';
import { getAllContracts, parseContract } from '../../api';
import { getUserKeywords, batchCreateKeywords } from '../../api/customKeywords';
import styles from './styles.module.css';
import type { PrepaymentItem } from '../../components/ContractConfirmModal/types';

const { Dragger } = Upload;
const { Title } = Typography;

const PageA: React.FC<PageAProps> = () => {
  const navigate = useNavigate();
  
  // 状态管理
  const [uploadedFiles, setUploadedFiles] = useState<UploadFile[]>([]);
  const [contractList, setContractList] = useState<ContractData[]>([]);
  const [loading, setLoading] = useState(false);
  const [customFields, setCustomFields] = useState<string[]>([]);
  const [customFieldInput, setCustomFieldInput] = useState<string>('');
  const [hasInitialized, setHasInitialized] = useState(false);
  
  // 加载合同列表和自定义关键字
  useEffect(() => {
    // 防止React.StrictMode导致的重复调用
    if (hasInitialized) return;
    const fetchContracts = async () => {
      setLoading(true);
      try {
        const response = await getAllContracts();
        setContractList(response.contracts);
      } catch (error) {
        console.error('获取合同列表失败:', error);
        message.error('合同列表加载失败，请稍后重试');
      } finally {
        setLoading(false);
      }
    };

    const fetchCustomKeywords = async () => {
      try {
        const response = await getUserKeywords();
        // 将 response 中的 keyword 提取成数组
        const keywords = response.map(item => item.keyword);
        setCustomFields(keywords);
      } catch (error) {
        console.error('获取自定义关键字失败:', error);
        // 不显示错误提示，静默失败
      }
    };

    fetchContracts();
    fetchCustomKeywords();
    setHasInitialized(true);
  }, [hasInitialized]);
  

  // 处理文件上传
  const handleFileUpload = useCallback(async (file: File) => {
    setLoading(true);
    try {
      // 步骤1：批量保存自定义关键字
      if (customFields.length > 0) {
        message.loading({ content: '正在保存自定义关键字...', key: 'keywords' });
        console.log('保存自定义关键字:', customFields);
        await batchCreateKeywords(customFields);
        message.success({ content: '自定义关键字保存成功', key: 'keywords', duration: 1 });
      }

      // 步骤2：调用解析合同文件接口（不保存到数据库）
      message.loading({ content: '正在解析合同文件...', key: 'parse' });
      console.log('开始解析文件:', file.name);
      const parseResponse = await parseContract(file);
      console.log('解析响应:', parseResponse);
      message.success({ content: parseResponse.message || '合同解析成功', key: 'parse' });

      // 步骤3：生成初始摊销明细（基于解析结果）
      const startDate = parseResponse.startDate;
      const endDate = parseResponse.endDate;
      const totalAmount = parseResponse.totalAmount;
      
      // 计算月份数
      const start = new Date(startDate);
      const end = new Date(endDate);
      const months = (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth()) + 1;
      
      // 生成摊销明细
      const monthlyAmount = totalAmount / months;
      const formattedEntries: PrepaymentItem[] = [];
      for (let i = 0; i < months; i++) {
        const periodDate = new Date(start.getFullYear(), start.getMonth() + i, 1);
        const period = `${periodDate.getFullYear()}-${String(periodDate.getMonth() + 1).padStart(2, '0')}`;
        formattedEntries.push({
          id: i,
          amortizationPeriod: period,
          accountingPeriod: period,
          amount: Number(monthlyAmount.toFixed(2)),
          paymentStatus: 'PENDING'
        });
      }
      console.log('生成的摊销明细:', formattedEntries);

      // 跳转到合同预览页面（此时还未保存到数据库）
      navigate('/contractPreview', {
        state: {
          contractInfo: parseResponse,
          prepaymentData: formattedEntries,
          isNewContract: true, // 标记这是新合同，需要保存
        },
      });
    } catch (error) {
      console.error('上传失败:', error);
      message.error('合同上传失败，请重试');
    } finally {
      setLoading(false);
    }
  }, [customFields, navigate]);

  // 处理文件上传变化
  function handleUploadChange(info: any) {
    setUploadedFiles(info.fileList);
  }

  // 处理拖拽放置
  const handleDrop = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    console.log('Dropped files', e.dataTransfer.files);
  }, []);

  // 表格列配置
  const columns: ColumnsType<ContractData> = [
    {
      title: '合同附件',
      dataIndex: 'attachmentName',
      key: 'attachmentName',
      width: '25%',
      render: (text: string) => (
        <Space>
          <FileTextOutlined style={{ color: '#1890ff' }} />
          <span>{text}</span>
        </Space>
      ),
    },
    {
      title: '供应商',
      dataIndex: 'vendorName',
      key: 'vendorName',
      width: '15%',
    },
    {
      title: '合同金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: '12%',
      render: (amount: number) => `¥${amount.toFixed(2)}`,
    },
    {
      title: '合同期限',
      key: 'dateRange',
      width: '18%',
      render: (_, record: ContractData) => (
        <span>
          {record.startDate} 至 {record.endDate}
        </span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: '10%',
      render: (status: string) => (
        <span style={{ color: status === 'ACTIVE' ? '#52c41a' : '#999' }}>
          {status === 'ACTIVE' ? '进行中' : '未生效'}
        </span>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      width: '20%',
      render: (_, record: ContractData) => (
        <Space className={styles.actionButtons}>
          <Button 
            type="primary" 
            size="small"
            icon={<EyeOutlined />}
            className={styles.actionButton}
            onClick={() => handleView(record)}
          >
            查看
          </Button>
          <Button 
            danger 
            size="small"
            icon={<DeleteOutlined />}
            className={styles.actionButton}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  // 操作处理函数
  const handleView = useCallback((record: ContractData) => {
    // 跳转到合同详情页，并将合同ID放入URL
    navigate(`/contract/${record.contractId}`);
  }, [navigate]);

  const handleDelete = useCallback((record: ContractData) => {
    setLoading(true);
    // 模拟删除操作
    setTimeout(() => {
      setContractList(prev => prev.filter(item => item.contractId !== record.contractId));
      message.success(`已删除合同: ${record.attachmentName}`);
      setLoading(false);
    }, 1000);
  }, []);


  // 文件上传配置
  const uploadProps: UploadProps = {
    name: 'file',
    multiple: false,
    accept: '.pdf,.doc,.docx,.jpg,.jpeg,.png',
    maxCount: 1,
    beforeUpload: (file) => {
      // 拦截上传，手动调用接口
      handleFileUpload(file);
      return false; // 阻止默认上传行为
    },
    onChange: handleUploadChange,
    onDrop: handleDrop,
    fileList: uploadedFiles,
    showUploadList: {
      showDownloadIcon: false,
      showRemoveIcon: true,
      showPreviewIcon: false,
    },
  };

  // 合同分析字段配置
  const analysisFields = [
    { label: '合同名称', value: 'contractName', disabled: true, checked: true },
    { label: '供应商名称', value: 'vendorName', disabled: true, checked: true },
    { label: '合同总金额', value: 'totalAmount', disabled: true, checked: true },
    { label: '合同开始时间', value: 'startDate', disabled: true, checked: true },
    { label: '合同结束时间', value: 'endDate', disabled: true, checked: true },
    { label: '税率', value: 'taxRate', disabled: true, checked: true },
  ];

  // 处理自定义字段添加
  const handleCustomFieldAdd = () => {
    const trimmedValue = customFieldInput.trim();
    if (trimmedValue && !customFields.includes(trimmedValue)) {
      setCustomFields([...customFields, trimmedValue]);
      setCustomFieldInput('');
    } else if (customFields.includes(trimmedValue)) {
      message.warning('该字段已存在');
    }
  };

  // 处理自定义字段删除
  const handleCustomFieldRemove = (fieldToRemove: string) => {
    setCustomFields(customFields.filter(field => field !== fieldToRemove));
  };

  return (
    <div className={styles.pageContainer}>
      {/* 区域A - 文件上传区域 */}
      <div className={styles.uploadArea}>
        {/* 左侧：合同解析内容区 */}
        <div className={styles.leftSection}>
          <div className={styles.sectionHeader}>
            <Title level={4} style={{ margin: 0, color: '#262626' }}>
              合同解析内容
            </Title>
          </div>
          <div className={styles.checkboxContainer}>
            {analysisFields.map((field) => (
              <div key={field.value} className={styles.checkboxItem}>
                <Checkbox
                  checked={field.checked}
                  disabled={field.disabled}
                >
                  {field.label}
                </Checkbox>
              </div>
            ))}
          </div>
          
          {/* 自定义字段标签展示区域 */}
          {customFields.length > 0 && (
            <div className={styles.customTagsContainer}>
              {customFields.map((field) => (
                <Tag
                  key={field}
                  closable
                  onClose={() => handleCustomFieldRemove(field)}
                  color="blue"
                >
                  {field}
                </Tag>
              ))}
            </div>
          )}
          
          {/* 自定义字段输入框 */}
          <div className={styles.customInputContainer}>
            <Input
              placeholder="自定义"
              value={customFieldInput}
              onChange={(e) => setCustomFieldInput(e.target.value)}
              onPressEnter={handleCustomFieldAdd}
              style={{ width: '100%', backgroundColor: '#ffffff' }}
              prefix={<span style={{ color: '#999', marginRight: 4 }}>自定义：</span>}
            />
          </div>
        </div>

        {/* 右侧：文件上传功能 */}
        <div className={styles.rightSection}>
          <Dragger {...uploadProps} className={styles.uploadDragger}>
            <p className={styles.uploadIcon}>
              <InboxOutlined />
            </p>
            <p className={styles.uploadText}>
              点击或拖拽文件到此区域上传
            </p>
            <p className={styles.uploadHint}>
              支持单个 PDF 格式
            </p>
          </Dragger>
        </div>
      </div>

      {/* 区域B - 合同列表区域 */}
      <div className={styles.contractTableArea}>
        <div className={styles.tableContainer}>
          <div style={{ marginBottom: '16px' }}>
            <Title level={4} style={{ margin: 0, color: '#262626' }}>
              合同列表
            </Title>
          </div>
          <Spin spinning={loading}>
            <Table
              columns={columns}
              dataSource={contractList}
              rowKey="id"
              pagination={false}
              locale={{
                emptyText: (
                  <div className={styles.emptyState}>
                    <FileTextOutlined className={styles.emptyIcon} />
                    <div className={styles.emptyText}>暂无合同数据</div>
                    <div className={styles.emptyHint}>请先上传合同文件</div>
                  </div>
                )
              }}
            />
            
          </Spin>
        </div>
      </div>
    </div>
  );
};

export default PageA;
