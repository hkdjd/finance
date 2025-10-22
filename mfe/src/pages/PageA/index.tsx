import React, { useState, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Upload, 
  Table, 
  Button, 
  Space, 
  message, 
  Spin,
  Empty,
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
import { getAllContracts, uploadContract, calculateAmortization } from '../../api';
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
  
  // 加载合同列表
  useEffect(() => {
    const fetchContracts = async () => {
      setLoading(true);
      try {
        const response = await getAllContracts();
        setContractList(response.contracts);
        message.success(response.message || '合同列表加载成功');
      } catch (error) {
        console.error('获取合同列表失败:', error);
        message.error('合同列表加载失败，请稍后重试');
      } finally {
        setLoading(false);
      }
    };

    fetchContracts();
  }, []);
  

  // 处理文件上传
  const handleFileUpload = useCallback(async (file: File) => {
    setLoading(true);
    try {
      // 步骤2：调用上传合同文件接口
      message.loading({ content: '正在上传合同文件...', key: 'upload' });
      console.log('开始上传文件:', file.name);
      const uploadResponse = await uploadContract(file);
      console.log('上传响应:', uploadResponse);
      message.success({ content: uploadResponse.message || '合同上传成功', key: 'upload' });

      // 步骤3：根据 contractId 调用计算合同摘销明细接口
      message.loading({ content: '正在计算摘销明细...', key: 'calculate' });
      console.log('开始计算摘销明细, contractId:', uploadResponse.contractId);
      const amortizationResponse = await calculateAmortization(uploadResponse.contractId);
      console.log('摘销计算响应:', amortizationResponse);
      message.success({ content: '摘销明细计算完成', key: 'calculate' });

      // 步骤4：将摊销明细设置到弹窗中
      const formattedEntries = amortizationResponse.entries.map((entry, index) => ({
        ...entry,
        id: entry.id ?? index,
      })) as PrepaymentItem[];
      console.log('格式化后的摘销明细:', formattedEntries);

      // 跳转到合同预览页面
      navigate('/contractPreview', {
        state: {
          contractInfo: uploadResponse,
          prepaymentData: formattedEntries,
        },
      });

      // 刷新合同列表
      const listResponse = await getAllContracts();
      setContractList(listResponse.contracts);
    } catch (error) {
      console.error('上传失败:', error);
      message.error('合同上传失败，请重试');
    } finally {
      setLoading(false);
    }
  }, []);

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
          {status === 'ACTIVE' ? '激活' : '非激活'}
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
              支持单个或批量上传，支持 PDF、Word、Excel、TXT 格式
            </p>
          </Dragger>
        </div>
      </div>

      {/* 区域B - 合同列表区域 */}
      <div className={styles.contractTableArea}>
        <div className={styles.tableContainer}>
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
