import {
  AmortizationListResponse,
  AmortizationOperateRequest,
  AmortizationOperateResponse,
  AmortizationEntryDetail,
  PaymentStatus,
} from './types';

/**
 * Mock 摊销明细列表数据
 */
export const mockAmortizationListData: Record<number, AmortizationListResponse> = {
  1: {
    contract: {
      id: 1,
      totalAmount: 6000.0,
      startDate: '2024-01-01',
      endDate: '2024-06-30',
      vendorName: '供应商A',
    },
    amortization: [
      {
        id: 1,
        amortizationPeriod: '2024-01',
        accountingPeriod: '2024-01',
        amount: 1000.0,
        periodDate: '2024-01-01',
        paymentStatus: PaymentStatus.COMPLETED,
        createdAt: '2024-01-24T14:30:52.123456+08:00',
        updatedAt: '2024-01-24T14:30:52.123456+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 2,
        amortizationPeriod: '2024-02',
        accountingPeriod: '2024-02',
        amount: 1000.0,
        periodDate: '2024-02-01',
        paymentStatus: PaymentStatus.COMPLETED,
        createdAt: '2024-01-24T14:30:52.123456+08:00',
        updatedAt: '2024-02-15T10:20:30.123456+08:00',
        createdBy: 'system',
        updatedBy: 'admin',
      },
      {
        id: 3,
        amortizationPeriod: '2024-03',
        accountingPeriod: '2024-03',
        amount: 1000.0,
        periodDate: '2024-03-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-01-24T14:30:52.123456+08:00',
        updatedAt: '2024-01-24T14:30:52.123456+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 4,
        amortizationPeriod: '2024-04',
        accountingPeriod: '2024-04',
        amount: 1000.0,
        periodDate: '2024-04-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-01-24T14:30:52.123456+08:00',
        updatedAt: '2024-01-24T14:30:52.123456+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 5,
        amortizationPeriod: '2024-05',
        accountingPeriod: '2024-05',
        amount: 1000.0,
        periodDate: '2024-05-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-01-24T14:30:52.123456+08:00',
        updatedAt: '2024-01-24T14:30:52.123456+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 6,
        amortizationPeriod: '2024-06',
        accountingPeriod: '2024-06',
        amount: 1000.0,
        periodDate: '2024-06-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-01-24T14:30:52.123456+08:00',
        updatedAt: '2024-01-24T14:30:52.123456+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
    ],
  },
  2: {
    contract: {
      id: 2,
      totalAmount: 7500.0,
      startDate: '2024-02-01',
      endDate: '2024-07-31',
      vendorName: '供应商B',
    },
    amortization: [
      {
        id: 7,
        amortizationPeriod: '2024-02',
        accountingPeriod: '2024-02',
        amount: 1250.0,
        periodDate: '2024-02-01',
        paymentStatus: PaymentStatus.COMPLETED,
        createdAt: '2024-02-24T14:30:52.789012+08:00',
        updatedAt: '2024-02-24T14:30:52.789012+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 8,
        amortizationPeriod: '2024-03',
        accountingPeriod: '2024-03',
        amount: 1250.0,
        periodDate: '2024-03-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-02-24T14:30:52.789012+08:00',
        updatedAt: '2024-02-24T14:30:52.789012+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 9,
        amortizationPeriod: '2024-04',
        accountingPeriod: '2024-04',
        amount: 1250.0,
        periodDate: '2024-04-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-02-24T14:30:52.789012+08:00',
        updatedAt: '2024-02-24T14:30:52.789012+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 10,
        amortizationPeriod: '2024-05',
        accountingPeriod: '2024-05',
        amount: 1250.0,
        periodDate: '2024-05-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-02-24T14:30:52.789012+08:00',
        updatedAt: '2024-02-24T14:30:52.789012+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 11,
        amortizationPeriod: '2024-06',
        accountingPeriod: '2024-06',
        amount: 1250.0,
        periodDate: '2024-06-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-02-24T14:30:52.789012+08:00',
        updatedAt: '2024-02-24T14:30:52.789012+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 12,
        amortizationPeriod: '2024-07',
        accountingPeriod: '2024-07',
        amount: 1250.0,
        periodDate: '2024-07-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-02-24T14:30:52.789012+08:00',
        updatedAt: '2024-02-24T14:30:52.789012+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
    ],
  },
  3: {
    contract: {
      id: 3,
      totalAmount: 8000.0,
      startDate: '2024-03-01',
      endDate: '2024-08-31',
      vendorName: '供应商C',
    },
    amortization: [
      {
        id: 13,
        amortizationPeriod: '2024-03',
        accountingPeriod: '2024-03',
        amount: 1333.33,
        periodDate: '2024-03-01',
        paymentStatus: PaymentStatus.COMPLETED,
        createdAt: '2024-03-24T15:02:30.123456+08:00',
        updatedAt: '2024-03-24T15:02:30.123456+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 14,
        amortizationPeriod: '2024-04',
        accountingPeriod: '2024-04',
        amount: 1333.33,
        periodDate: '2024-04-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-03-24T15:02:30.123456+08:00',
        updatedAt: '2024-03-24T15:02:30.123456+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 15,
        amortizationPeriod: '2024-05',
        accountingPeriod: '2024-05',
        amount: 1333.33,
        periodDate: '2024-05-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-03-24T15:02:30.123456+08:00',
        updatedAt: '2024-03-24T15:02:30.123456+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 16,
        amortizationPeriod: '2024-06',
        accountingPeriod: '2024-06',
        amount: 1333.33,
        periodDate: '2024-06-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-03-24T15:02:30.123456+08:00',
        updatedAt: '2024-03-24T15:02:30.123456+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 17,
        amortizationPeriod: '2024-07',
        accountingPeriod: '2024-07',
        amount: 1333.33,
        periodDate: '2024-07-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-03-24T15:02:30.123456+08:00',
        updatedAt: '2024-03-24T15:02:30.123456+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
      {
        id: 18,
        amortizationPeriod: '2024-08',
        accountingPeriod: '2024-08',
        amount: 1333.35,
        periodDate: '2024-08-01',
        paymentStatus: PaymentStatus.PENDING,
        createdAt: '2024-03-24T15:02:30.123456+08:00',
        updatedAt: '2024-03-24T15:02:30.123456+08:00',
        createdBy: 'system',
        updatedBy: 'system',
      },
    ],
  },
};

/**
 * 生成摊销明细列表 Mock 数据
 * @param contractId 合同ID
 */
export const getMockAmortizationList = (contractId: number): AmortizationListResponse => {
  const data = mockAmortizationListData[contractId];
  
  if (!data) {
    // 如果没找到对应的合同，返回空数据
    return {
      contract: {
        id: contractId,
        totalAmount: 0,
        startDate: '',
        endDate: '',
        vendorName: '未知供应商',
      },
      amortization: [],
    };
  }
  
  return data;
};

/**
 * 生成摊销明细操作 Mock 响应
 * @param request 操作请求
 */
export const getMockAmortizationOperateResponse = (
  request: AmortizationOperateRequest
): AmortizationOperateResponse => {
  const { contractId, amortization } = request;
  
  // 从 mock 数据中获取合同信息
  const existingData = mockAmortizationListData[contractId];
  
  // if (!existingData) {
  //   throw new Error(`未找到合同，ID=${contractId}`);
  // }
  
  const timestamp = new Date().toISOString();
  const currentUser = 'admin'; // 模拟当前用户
  
  // 处理操作逻辑：
  // 1. ID 为 null -> 新增 (分配新 ID)
  // 2. ID 存在 -> 更新
  // 3. 原有数据中存在但请求中不存在的 ID -> 删除
  
  let maxId = Math.max(
    ...Object.values(mockAmortizationListData)
      .flatMap((data) => data.amortization.map((entry) => entry.id || 0))
  );
  
  const processedEntries: AmortizationEntryDetail[] = amortization.map((entry) => {
    if (entry.id === null) {
      // 新增：分配新 ID
      maxId += 1;
      return {
        ...entry,
        id: maxId,
        createdAt: timestamp,
        updatedAt: timestamp,
        createdBy: currentUser,
        updatedBy: currentUser,
      };
    } else {
      // 更新：保留创建信息，更新修改信息
      const existingEntry = existingData?.amortization?.find((e) => e.id === entry.id);
      return {
        ...entry,
        createdAt: existingEntry?.createdAt || timestamp,
        updatedAt: timestamp,
        createdBy: existingEntry?.createdBy || currentUser,
        updatedBy: currentUser,
      };
    }
  });
  
  // 更新 mock 数据（实际应用中不需要，这里仅为演示）
  mockAmortizationListData[contractId] = {
    contract: existingData?.contract,
    amortization: processedEntries,
  };
  
  return {
    contract: existingData?.contract,
    amortization: processedEntries,
  };
};
