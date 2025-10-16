// 预提会计分录Mock数据

import { JournalEntriesPreviewResponse } from './types';

// Mock数据 - 预提会计分录预览
export const mockJournalEntriesPreview: JournalEntriesPreviewResponse = {
  contract: {
    id: 1,
    totalAmount: 3000.00,
    startDate: "2024-01-01",
    endDate: "2024-03-31",
    vendorName: "供应商A"
  },
  previewEntries: [
    {
      entryType: "AMORTIZATION",
      bookingDate: "2024-01-31",
      accountName: "费用",
      debitAmount: 1000.00,
      creditAmount: 0.00,
      description: "摊销费用预览",
      memo: "摊销费用预览 - 2024-01",
      entryOrder: 1
    },
    {
      entryType: "AMORTIZATION",
      bookingDate: "2024-01-31",
      accountName: "应付",
      debitAmount: 0.00,
      creditAmount: 1000.00,
      description: "摊销应付预览",
      memo: "摊销应付预览 - 2024-01",
      entryOrder: 2
    },
    {
      entryType: "AMORTIZATION",
      bookingDate: "2024-02-29",
      accountName: "费用",
      debitAmount: 1000.00,
      creditAmount: 0.00,
      description: "摊销费用预览",
      memo: "摊销费用预览 - 2024-02",
      entryOrder: 3
    },
    {
      entryType: "AMORTIZATION",
      bookingDate: "2024-02-29",
      accountName: "应付",
      debitAmount: 0.00,
      creditAmount: 1000.00,
      description: "摊销应付预览",
      memo: "摊销应付预览 - 2024-02",
      entryOrder: 4
    },
    {
      entryType: "AMORTIZATION",
      bookingDate: "2024-03-31",
      accountName: "费用",
      debitAmount: 1000.00,
      creditAmount: 0.00,
      description: "摊销费用预览",
      memo: "摊销费用预览 - 2024-03",
      entryOrder: 5
    },
    {
      entryType: "AMORTIZATION",
      bookingDate: "2024-03-31",
      accountName: "应付",
      debitAmount: 0.00,
      creditAmount: 1000.00,
      description: "摊销应付预览",
      memo: "摊销应付预览 - 2024-03",
      entryOrder: 6
    }
  ]
};

// 付款会计分录Mock数据
export const mockPaymentJournalEntriesPreview: JournalEntriesPreviewResponse = {
  contract: {
    id: 1,
    totalAmount: 3000.00,
    startDate: "2024-01-01",
    endDate: "2024-03-31",
    vendorName: "供应商A"
  },
  previewEntries: [
    {
      entryType: "PAYMENT",
      bookingDate: "2024-01-15",
      accountName: "应付账款",
      debitAmount: 1000.00,
      creditAmount: 0.00,
      description: "付款冲减应付",
      memo: "付款冲减应付账款 - 第1期",
      entryOrder: 1
    },
    {
      entryType: "PAYMENT",
      bookingDate: "2024-01-15",
      accountName: "银行存款",
      debitAmount: 0.00,
      creditAmount: 1000.00,
      description: "银行付款",
      memo: "银行转账付款 - 第1期",
      entryOrder: 2
    },
    {
      entryType: "PAYMENT",
      bookingDate: "2024-02-15",
      accountName: "应付账款",
      debitAmount: 1000.00,
      creditAmount: 0.00,
      description: "付款冲减应付",
      memo: "付款冲减应付账款 - 第2期",
      entryOrder: 3
    },
    {
      entryType: "PAYMENT",
      bookingDate: "2024-02-15",
      accountName: "银行存款",
      debitAmount: 0.00,
      creditAmount: 1000.00,
      description: "银行付款",
      memo: "银行转账付款 - 第2期",
      entryOrder: 4
    },
    {
      entryType: "PAYMENT",
      bookingDate: "2024-03-15",
      accountName: "应付账款",
      debitAmount: 1000.00,
      creditAmount: 0.00,
      description: "付款冲减应付",
      memo: "付款冲减应付账款 - 第3期",
      entryOrder: 5
    },
    {
      entryType: "PAYMENT",
      bookingDate: "2024-03-15",
      accountName: "银行存款",
      debitAmount: 0.00,
      creditAmount: 1000.00,
      description: "银行付款",
      memo: "银行转账付款 - 第3期",
      entryOrder: 6
    }
  ]
};

// 模拟不同合同和不同类型的数据
export const mockJournalEntriesPreviewMap: Record<string, JournalEntriesPreviewResponse> = {
  // 摊销类型数据
  "1_AMORTIZATION": mockJournalEntriesPreview,
  "2_AMORTIZATION": {
    contract: {
      id: 2,
      totalAmount: 6000.00,
      startDate: "2024-04-01",
      endDate: "2024-06-30",
      vendorName: "供应商B"
    },
    previewEntries: [
      {
        entryType: "AMORTIZATION",
        bookingDate: "2024-04-30",
        accountName: "费用",
        debitAmount: 2000.00,
        creditAmount: 0.00,
        description: "摊销费用预览",
        memo: "摊销费用预览 - 2024-04",
        entryOrder: 1
      },
      {
        entryType: "AMORTIZATION",
        bookingDate: "2024-04-30",
        accountName: "应付",
        debitAmount: 0.00,
        creditAmount: 2000.00,
        description: "摊销应付预览",
        memo: "摊销应付预览 - 2024-04",
        entryOrder: 2
      }
    ]
  },
  // 付款类型数据
  "1_PAYMENT": mockPaymentJournalEntriesPreview,
  "2_PAYMENT": {
    contract: {
      id: 2,
      totalAmount: 6000.00,
      startDate: "2024-04-01",
      endDate: "2024-06-30",
      vendorName: "供应商B"
    },
    previewEntries: [
      {
        entryType: "PAYMENT",
        bookingDate: "2024-04-15",
        accountName: "应付账款",
        debitAmount: 2000.00,
        creditAmount: 0.00,
        description: "付款冲减应付",
        memo: "付款冲减应付账款 - 供应商B",
        entryOrder: 1
      },
      {
        entryType: "PAYMENT",
        bookingDate: "2024-04-15",
        accountName: "银行存款",
        debitAmount: 0.00,
        creditAmount: 2000.00,
        description: "银行付款",
        memo: "银行转账付款 - 供应商B",
        entryOrder: 2
      }
    ]
  }
};
