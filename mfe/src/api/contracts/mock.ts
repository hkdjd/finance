import { 
  ContractsListResponse, 
  ContractStatus, 
  ContractUploadResponse,
  AmortizationCalculateResponse,
  AmortizationScenario,
  AmortizationStatus,
  AmortizationEntry,
  ContractAmortizationResponse,
  PaymentStatus,
  PaymentExecuteRequest,
  PaymentExecuteResponse,
  ContractPaymentRecordsResponse,
  UpdateContractRequest,
  UpdateContractResponse,
  AuditLogResponse
} from './types';

/**
 * Mock 合同列表数据
 */
export const mockContractsList: ContractsListResponse = {
  contracts: [
    {
      contractId: 3,
      totalAmount: 8000.0,
      startDate: '2024-03-01',
      endDate: '2024-08-31',
      vendorName: '供应商C',
      attachmentName: 'contract_20240324_150230_x9y8z7w6.pdf',
      createdAt: '2024-03-24T15:02:30.123456+08:00',
      status: ContractStatus.ACTIVE,
    },
    {
      contractId: 2,
      totalAmount: 7500.0,
      startDate: '2024-02-01',
      endDate: '2024-07-31',
      vendorName: '供应商B',
      attachmentName: 'contract_20240224_143052_b2c3d4e5.pdf',
      createdAt: '2024-02-24T14:30:52.789012+08:00',
      status: ContractStatus.ACTIVE,
    },
    {
      contractId: 1,
      totalAmount: 6000.0,
      startDate: '2024-01-01',
      endDate: '2024-06-30',
      vendorName: '供应商A',
      attachmentName: 'contract_20240124_143052_a1b2c3d4.pdf',
      createdAt: '2024-01-24T14:30:52.123456+08:00',
      status: ContractStatus.ACTIVE,
    },
  ],
  totalCount: 3,
  message: '查询成功',
};

/**
 * 生成分页 Mock 数据
 * @param page 页码
 * @param size 每页大小
 */
export const getMockContractsListPaginated = (
  page: number = 0,
  size: number = 10
): ContractsListResponse => {
  const start = page * size;
  const end = start + size;
  const contracts = mockContractsList.contracts.slice(start, end);

  return {
    contracts,
    totalCount: mockContractsList.totalCount,
    message: '查询成功',
  };
};

/**
 * 生成合同上传 Mock 数据
 * @param fileName 文件名
 */
export const getMockContractUploadResponse = (fileName: string): ContractUploadResponse => {
  const timestamp = new Date().toISOString();
  const randomId = Math.floor(Math.random() * 10000) + 1;
  const year = new Date().getFullYear();
  const month = String(new Date().getMonth() + 1).padStart(2, '0');
  const fileNameWithoutExt = fileName.split('.')[0];
  const uniqueFileName = `contract_${Date.now()}_${Math.random().toString(36).substring(7)}.pdf`;
  
  return {
    contractId: randomId,
    totalAmount: 6000.0 + Math.random() * 4000, // 随机金额 6000-10000
    startDate: '2024-01-01',
    endDate: '2024-06-30',
    taxRate: 0.06,
    vendorName: `测试供应商_${fileNameWithoutExt}`,
    attachmentName: uniqueFileName,
    attachmentPath: ``,
    createdAt: timestamp,
    message: '合同上传和解析成功',
    customFields: {
      '法人': '张三',
      '项目名称': '测试项目',
      '合同编号': `HT-${year}-${String(randomId).padStart(3, '0')}`
    }
  };
};

/**
 * 生成摊销计算 Mock 数据
 * @param contractId 合同ID
 */
export const getMockAmortizationCalculate = (contractId: number): AmortizationCalculateResponse => {
  // 从 mock 合同列表中查找对应合同
  const contract = mockContractsList.contracts.find(c => c.contractId === contractId);
  
  if (!contract) {
    // 如果没找到，返回默认数据
    return {
      totalAmount: 6000.0,
      startDate: '2024-01',
      endDate: '2024-06',
      scenario: AmortizationScenario.SCENARIO_1,
      generatedAt: new Date().toISOString(),
      entries: [
        { id: null, amortizationPeriod: '2024-01', accountingPeriod: '2024-01', amount: 1000.0, status: AmortizationStatus.PENDING },
        { id: null, amortizationPeriod: '2024-02', accountingPeriod: '2024-02', amount: 1000.0, status: AmortizationStatus.PENDING },
        { id: null, amortizationPeriod: '2024-03', accountingPeriod: '2024-03', amount: 1000.0, status: AmortizationStatus.PENDING },
        { id: null, amortizationPeriod: '2024-04', accountingPeriod: '2024-04', amount: 1000.0, status: AmortizationStatus.PENDING },
        { id: null, amortizationPeriod: '2024-05', accountingPeriod: '2024-05', amount: 1000.0, status: AmortizationStatus.PENDING },
        { id: null, amortizationPeriod: '2024-06', accountingPeriod: '2024-06', amount: 1000.0, status: AmortizationStatus.PENDING },
      ],
    };
  }

  // 根据合同信息生成摊销明细
  const startDate = new Date(contract.startDate);
  const endDate = new Date(contract.endDate);
  
  // 计算月份差
  const monthDiff = (endDate.getFullYear() - startDate.getFullYear()) * 12 + 
                    (endDate.getMonth() - startDate.getMonth()) + 1;
  
  // 计算每月摊销金额
  const monthlyAmount = contract.totalAmount / monthDiff;
  
  // 生成摊销明细条目
  const entries: AmortizationEntry[] = [];
  for (let i = 0; i < monthDiff; i++) {
    const currentDate = new Date(startDate);
    currentDate.setMonth(startDate.getMonth() + i);
    const period = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}`;
    
    entries.push({
      id: null,
      amortizationPeriod: period,
      accountingPeriod: period,
      amount: parseFloat(monthlyAmount.toFixed(2)),
      status: AmortizationStatus.PENDING,
    });
  }

  return {
    totalAmount: contract.totalAmount,
    startDate: `${startDate.getFullYear()}-${String(startDate.getMonth() + 1).padStart(2, '0')}`,
    endDate: `${endDate.getFullYear()}-${String(endDate.getMonth() + 1).padStart(2, '0')}`,
    scenario: AmortizationScenario.SCENARIO_1,
    generatedAt: new Date().toISOString(),
    entries,
  };
};

/**
 * 生成合同摊销明细列表 Mock 数据
 * @param contractId 合同ID
 */
export const getMockContractAmortizationEntries = (contractId: number): ContractAmortizationResponse => {
  // 根据 contractId 返回不同的 mock 数据
  if (contractId === 1) {
    return {
      contract: {
        id: 1,
        totalAmount: 4000.00,
        startDate: "2025-01-01",
        endDate: "2025-04-30",
        vendorName: "供应商A",
        taxRate: 0.06,
        attachmentName: "contract_20250101_001.pdf",
        attachmentPath: "http://localhost:8081/contracts/1/attachment?download=true",
        customFields: {
          '合同编号': 'HT-2025-001',
          '法人': '张三',
          '项目名称': '办公设备租赁项目'
        }
      },
      amortization: [
        {
          id: 1,
          amortizationPeriod: "2025-01",
          accountingPeriod: "2025-01",
          amount: 1000.00,
          periodDate: "2025-01-01",
          paymentStatus: PaymentStatus.PENDING,
          createdAt: "2024-12-24T14:30:52.123456",
          updatedAt: "2024-12-24T14:30:52.123456",
          createdBy: "system",
          updatedBy: "system"
        },
        {
          id: 2,
          amortizationPeriod: "2025-02",
          accountingPeriod: "2025-02",
          amount: 1000.00,
          periodDate: "2025-02-01",
          paymentStatus: PaymentStatus.PENDING,
          createdAt: "2024-12-24T14:30:52.123456",
          updatedAt: "2024-12-24T14:30:52.123456",
          createdBy: "system",
          updatedBy: "system"
        },
        {
          id: 3,
          amortizationPeriod: "2025-03",
          accountingPeriod: "2025-03",
          amount: 1000.00,
          periodDate: "2025-03-01",
          paymentStatus: PaymentStatus.PENDING,
          createdAt: "2024-12-24T14:30:52.123456",
          updatedAt: "2024-12-24T14:30:52.123456",
          createdBy: "system",
          updatedBy: "system"
        },
        {
          id: 4,
          amortizationPeriod: "2025-04",
          accountingPeriod: "2025-04",
          amount: 1000.00,
          periodDate: "2025-04-01",
          paymentStatus: PaymentStatus.PENDING,
          createdAt: "2024-12-24T14:30:52.123456",
          updatedAt: "2024-12-24T14:30:52.123456",
          createdBy: "system",
          updatedBy: "system"
        }
      ]
    };
  }

  if (contractId === 2) {
    return {
      contract: {
        id: 2,
        totalAmount: 7500.00,
        startDate: "2024-02-01",
        endDate: "2024-07-31",
        vendorName: "美的空调租赁有限公司",
        taxRate: 0.13,
        attachmentName: "contract_20240201_002.pdf",
        attachmentPath: "http://localhost:8081/contracts/2/attachment?download=true",
        customFields: {
          '合同编号': 'HT-2024-085',
          '法人': '李四',
          '项目名称': '空调租赁服务',
          '部门': '行政部'
        }
      },
      amortization: [
        {
          id: 5,
          amortizationPeriod: "2024-02",
          accountingPeriod: "2024-02",
          amount: 1250.00,
          periodDate: "2024-02-01",
          paymentStatus: PaymentStatus.PAID,
          createdAt: "2024-01-24T14:30:52.123456",
          updatedAt: "2024-02-15T10:20:30.123456",
          createdBy: "system",
          updatedBy: "admin"
        },
        {
          id: 6,
          amortizationPeriod: "2024-03",
          accountingPeriod: "2024-03",
          amount: 1250.00,
          periodDate: "2024-03-01",
          paymentStatus: PaymentStatus.PAID,
          createdAt: "2024-01-24T14:30:52.123456",
          updatedAt: "2024-03-15T09:15:45.123456",
          createdBy: "system",
          updatedBy: "admin"
        },
        {
          id: 7,
          amortizationPeriod: "2024-04",
          accountingPeriod: "2024-04",
          amount: 1250.00,
          periodDate: "2024-04-01",
          paymentStatus: PaymentStatus.PENDING,
          createdAt: "2024-01-24T14:30:52.123456",
          updatedAt: "2024-01-24T14:30:52.123456",
          createdBy: "system",
          updatedBy: "system"
        },
        {
          id: 8,
          amortizationPeriod: "2024-05",
          accountingPeriod: "2024-05",
          amount: 1250.00,
          periodDate: "2024-05-01",
          paymentStatus: PaymentStatus.PENDING,
          createdAt: "2024-01-24T14:30:52.123456",
          updatedAt: "2024-01-24T14:30:52.123456",
          createdBy: "system",
          updatedBy: "system"
        },
        {
          id: 9,
          amortizationPeriod: "2024-06",
          accountingPeriod: "2024-06",
          amount: 1250.00,
          periodDate: "2024-06-01",
          paymentStatus: PaymentStatus.PENDING,
          createdAt: "2024-01-24T14:30:52.123456",
          updatedAt: "2024-01-24T14:30:52.123456",
          createdBy: "system",
          updatedBy: "system"
        },
        {
          id: 10,
          amortizationPeriod: "2024-07",
          accountingPeriod: "2024-07",
          amount: 1250.00,
          periodDate: "2024-07-01",
          paymentStatus: PaymentStatus.PENDING,
          createdAt: "2024-01-24T14:30:52.123456",
          updatedAt: "2024-01-24T14:30:52.123456",
          createdBy: "system",
          updatedBy: "system"
        }
      ]
    };
  }

  if (contractId === 3) {
    return {
      contract: {
        id: 3,
        totalAmount: 8000.00,
        startDate: "2024-03-01",
        endDate: "2024-08-31",
        vendorName: "设备供应商C",
        taxRate: 0.09,
        attachmentName: "contract_20240301_003.pdf",
        attachmentPath: "http://localhost:8081/contracts/3/attachment?download=true",
        customFields: {
          '合同编号': 'HT-2024-120',
          '法人': '王五',
          '项目名称': '设备采购项目',
          '部门': '采购部',
          '联系人': '赵经理'
        }
      },
      amortization: [
        {
          id: 11,
          amortizationPeriod: "2024-03",
          accountingPeriod: "2024-03",
          amount: 1333.33,
          periodDate: "2024-03-01",
          paymentStatus: PaymentStatus.PAID,
          createdAt: "2024-02-24T14:30:52.123456",
          updatedAt: "2024-03-20T11:45:30.123456",
          createdBy: "system",
          updatedBy: "admin"
        },
        {
          id: 12,
          amortizationPeriod: "2024-04",
          accountingPeriod: "2024-04",
          amount: 1333.33,
          periodDate: "2024-04-01",
          paymentStatus: PaymentStatus.PAID,
          createdAt: "2024-02-24T14:30:52.123456",
          updatedAt: "2024-04-18T16:30:15.123456",
          createdBy: "system",
          updatedBy: "admin"
        },
        {
          id: 13,
          amortizationPeriod: "2024-05",
          accountingPeriod: "2024-05",
          amount: 1333.33,
          periodDate: "2024-05-01",
          paymentStatus: PaymentStatus.OVERDUE,
          createdAt: "2024-02-24T14:30:52.123456",
          updatedAt: "2024-05-25T08:15:45.123456",
          createdBy: "system",
          updatedBy: "system"
        },
        {
          id: 14,
          amortizationPeriod: "2024-06",
          accountingPeriod: "2024-06",
          amount: 1333.33,
          periodDate: "2024-06-01",
          paymentStatus: PaymentStatus.PENDING,
          createdAt: "2024-02-24T14:30:52.123456",
          updatedAt: "2024-02-24T14:30:52.123456",
          createdBy: "system",
          updatedBy: "system"
        },
        {
          id: 15,
          amortizationPeriod: "2024-07",
          accountingPeriod: "2024-07",
          amount: 1333.33,
          periodDate: "2024-07-01",
          paymentStatus: PaymentStatus.PENDING,
          createdAt: "2024-02-24T14:30:52.123456",
          updatedAt: "2024-02-24T14:30:52.123456",
          createdBy: "system",
          updatedBy: "system"
        },
        {
          id: 16,
          amortizationPeriod: "2024-08",
          accountingPeriod: "2024-08",
          amount: 1333.35,
          periodDate: "2024-08-01",
          paymentStatus: PaymentStatus.PENDING,
          createdAt: "2024-02-24T14:30:52.123456",
          updatedAt: "2024-02-24T14:30:52.123456",
          createdBy: "system",
          updatedBy: "system"
        }
      ]
    };
  }

  // 为其他合同ID提供默认数据
  const contract = mockContractsList.contracts.find(c => c.contractId === contractId);
  if (contract) {
    // 根据合同信息动态生成摊销明细
    const startDate = new Date(contract.startDate);
    const endDate = new Date(contract.endDate);
    const monthDiff = (endDate.getFullYear() - startDate.getFullYear()) * 12 + 
                      (endDate.getMonth() - startDate.getMonth()) + 1;
    const monthlyAmount = contract.totalAmount / monthDiff;
    
    const amortization = [];
    for (let i = 0; i < monthDiff; i++) {
      const currentDate = new Date(startDate);
      currentDate.setMonth(startDate.getMonth() + i);
      const period = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}`;
      const periodDate = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}-01`;
      
      amortization.push({
        id: i + 1,
        amortizationPeriod: period,
        accountingPeriod: period,
        amount: parseFloat(monthlyAmount.toFixed(2)),
        periodDate: periodDate,
        paymentStatus: PaymentStatus.PENDING,
        createdAt: "2024-12-24T14:30:52.123456",
        updatedAt: "2024-12-24T14:30:52.123456",
        createdBy: "system",
        updatedBy: "system"
      });
    }

    return {
      contract: {
        id: contract.contractId,
        totalAmount: contract.totalAmount,
        startDate: contract.startDate,
        endDate: contract.endDate,
        vendorName: contract.vendorName
      },
      amortization
    };
  }

  // 如果找不到合同，返回空数据
  return {
    contract: {
      id: contractId,
      totalAmount: 0,
      startDate: "",
      endDate: "",
      vendorName: "未知供应商"
    },
    amortization: []
  };
};

/**
 * Mock 支付执行响应
 * @param request 支付请求参数
 * @returns 支付执行响应
 */
export const getMockPaymentExecuteResponse = (request: PaymentExecuteRequest): PaymentExecuteResponse => {
  const { contractId, paymentAmount, paymentDate, selectedPeriods, operatorId } = request;
  
  // 生成选中账期的字符串格式
  const selectedPeriodsStr = selectedPeriods.map(period => {
    const year = 2024;
    const month = period.toString().padStart(2, '0');
    return `${year}-${month}`;
  });

  // 生成会计分录
  const journalEntries = selectedPeriods.map(period => {
    const year = 2024;
    const month = period.toString().padStart(2, '0');
    const periodStr = `${year}-${month}`;
    
    return [
      {
        bookingDate: `${year}-${month}-27`,
        account: '应付',
        dr: paymentAmount / selectedPeriods.length,
        cr: 0.00,
        memo: `period:${periodStr}`
      },
      {
        bookingDate: `${year}-${month}-27`,
        account: '预付',
        dr: 0.00,
        cr: paymentAmount / selectedPeriods.length,
        memo: `period:${periodStr}`
      }
    ];
  }).flat();

  return {
    paymentId: Math.floor(Math.random() * 1000) + 1,
    contractId,
    paymentAmount,
    bookingDate,
    selectedPeriods: selectedPeriodsStr,
    status: 'CONFIRMED',
    journalEntries,
    message: '付款执行成功'
  };
};

/**
 * Mock 合同支付记录列表响应
 * @param contractId 合同ID
 * @returns 合同支付记录列表
 */
export const getMockContractPaymentRecords = (contractId: number): ContractPaymentRecordsResponse => {
  return [
    {
      paymentId: 2,
      contractId: contractId,
      paymentAmount: 2001.00,
      bookingDate: "2024-03-25",
      selectedPeriods: ["2024-01", "2024-02"],
      status: "CONFIRMED",
      journalEntries: [
        {
          bookingDate: "2024-01-27",
          account: "应付",
          dr: 1000.00,
          cr: 0.00,
          memo: "period:2024-01"
        },
        {
          bookingDate: "2024-02-27",
          account: "应付",
          dr: 1000.00,
          cr: 0.00,
          memo: "period:2024-02"
        },
        {
          bookingDate: "2024-03-25",
          account: "费用",
          dr: 1.00,
          cr: 0.00,
          memo: "over small"
        },
        {
          bookingDate: "2024-03-25",
          account: "活期存款",
          dr: 0.00,
          cr: 2001.00,
          memo: "payment"
        }
      ]
    },
    {
      paymentId: 1,
      contractId: contractId,
      paymentAmount: 6000.00,
      bookingDate: "2024-03-20",
      selectedPeriods: ["2024-01", "2024-02", "2024-03", "2024-04", "2024-05", "2024-06"],
      status: "CONFIRMED",
      journalEntries: [
        {
          bookingDate: "2024-01-27",
          account: "应付",
          dr: 1000.00,
          cr: 0.00,
          memo: "period:2024-01"
        },
        {
          bookingDate: "2024-02-27",
          account: "应付",
          dr: 1000.00,
          cr: 0.00,
          memo: "period:2024-02"
        },
        {
          bookingDate: "2024-03-20",
          account: "预付",
          dr: 4000.00,
          cr: 0.00,
          memo: "over prepayment"
        },
        {
          bookingDate: "2024-03-20",
          account: "活期存款",
          dr: 0.00,
          cr: 6000.00,
          memo: "payment"
        }
      ]
    }
  ];
};
/*
 * 生成更新合同 Mock 数据
 * @param contractId 合同ID
 * @param request 更新请求参数
 */
export const getMockUpdateContractResponse = (
  contractId: number,
  request: UpdateContractRequest
): UpdateContractResponse => {
  // 从 mock 合同列表中查找对应合同
  const contract = mockContractsList.contracts.find(c => c.contractId === contractId);
  
  // 使用合同的原始附件名和创建时间，或使用默认值
  const attachmentName = contract?.attachmentName || `contract_${new Date().getTime()}.pdf`;
  const createdAt = contract?.createdAt || new Date().toISOString();
  
  // 从附件名生成路径
  const year = new Date(request.startDate).getFullYear();
  const month = String(new Date(request.startDate).getMonth() + 1).padStart(2, '0');
  const attachmentPath = `/uploads/contracts/${year}/${month}/${attachmentName}`;
  
  return {
    contractId,
    totalAmount: request.totalAmount,
    startDate: request.startDate,
    endDate: request.endDate,
    taxRate: request.taxRate,
    vendorName: request.vendorName,
    attachmentName,
    attachmentPath,
    createdAt,
    message: '合同信息更新成功',
    customFields: request.customFields
  };
};

/**
 * 生成审计日志 Mock 数据
 * @param amortizationEntryId 摊销明细ID
 */
export const getMockAuditLogResponse = (
  amortizationEntryId: number
): AuditLogResponse => {
  return {
    auditLogs: [
      {
        id: 1,
        amortizationEntryId,
        operationType: "PAYMENT",
        operationTypeDesc: "付款",
        operatorId: "user001",
        operationTime: "2024-10-23 10:15:30",
        paymentAmount: 1000.00,
        paymentDate: "2024-10-23",
        paymentStatus: "PAID",
        paymentStatusDesc: "已付款",
        oldPaymentAmount: undefined,
        oldPaymentDate: undefined,
        oldPaymentStatus: undefined,
        oldPaymentStatusDesc: undefined,
        remark: `付款操作：期间2024-10，付款金额1000.00`,
        createdAt: "2024-10-23 10:15:30",
        createdBy: "user001"
      },
      {
        id: 2,
        amortizationEntryId,
        operationType: "UPDATE",
        operationTypeDesc: "更新",
        operatorId: "user002",
        operationTime: "2024-10-22 14:30:15",
        paymentAmount: 800.00,
        paymentDate: "2024-10-22",
        paymentStatus: "PAID",
        paymentStatusDesc: "已付款",
        oldPaymentAmount: 500.00,
        oldPaymentDate: "2024-10-21",
        oldPaymentStatus: "PENDING",
        oldPaymentStatusDesc: "待付款",
        remark: "更新付款信息：调整付款金额和时间",
        createdAt: "2024-10-22 14:30:15",
        createdBy: "user002"
      }
    ],
    totalCount: 2,
    amortizationEntryId,
    message: "查询成功"
  };
};
