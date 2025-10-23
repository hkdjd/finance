// import React from 'react';
import { message } from 'antd';

/**
 * 会计分录不可变性工具类
 * 实现会计分录的累积性和不可变性原则
 */
export class JournalEntryImmutable {
  
  /**
   * 计算借贷总额和平衡状态
   * @param entries 会计分录数组
   * @returns 借贷总额和平衡状态
   */
  static calculateTotals(entries: any[]) {
    const debitTotal = entries.reduce((sum, entry) => sum + (entry.debitAmount || 0), 0);
    const creditTotal = entries.reduce((sum, entry) => sum + (entry.creditAmount || 0), 0);
    const isBalanced = Math.abs(debitTotal - creditTotal) < 0.01; // 允许0.01的误差
    
    return { debitTotal, creditTotal, isBalanced };
  }

  /**
   * 在支付成功后显示会计分录生成提示
   * @param entryCount 生成的分录数量
   */
  static showEntryGeneratedMessage(entryCount: number) {
    message.success({
      content: `✅ 支付成功！已自动生成 ${entryCount} 笔会计分录`,
      duration: 3,
      style: { marginTop: '20vh' }
    });
    
    // 延迟显示不可变性提示
    setTimeout(() => {
      message.info({
        content: '🔒 会计分录已确认，遵循不可变性原则',
        duration: 2,
        style: { marginTop: '20vh' }
      });
    }, 1500);
  }

  /**
   * 渲染会计分录不可变性提示组件
   */
  static renderImmutabilityNotice() {
    return (
      <div style={{
        marginBottom: 16,
        padding: '12px 16px',
        backgroundColor: '#FFF7ED',
        border: '1px solid #FDBA74',
        borderRadius: '8px',
        display: 'flex',
        alignItems: 'center',
        gap: '8px'
      }}>
        <span style={{ color: '#EA580C', fontSize: '16px' }}>⚠️</span>
        <div>
          <div style={{ color: '#EA580C', fontWeight: '600', fontSize: '14px' }}>
            会计分录不可变性原则
          </div>
          <div style={{ color: '#9A3412', fontSize: '13px', marginTop: '2px' }}>
            每次支付后生成的会计分录仅支持累加，已生成的分录无法删除或修改，确保审计追踪的完整性
          </div>
        </div>
      </div>
    );
  }

  /**
   * 渲染会计分录统计信息
   * @param entries 会计分录数组
   */
  static renderStatistics(entries: any[]) {
    const latestEntry = entries[entries.length - 1];
    const latestTime = latestEntry?.paymentTimestamp || latestEntry?.createdAt;
    
    return (
      <div style={{
        marginBottom: 16,
        padding: '16px 20px',
        backgroundColor: '#F8FAFC',
        borderRadius: '8px',
        border: '1px solid #E2E8F0'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', gap: '24px' }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ color: '#64748B', fontSize: '12px', marginBottom: '4px' }}>累计分录</div>
              <div style={{ color: '#1E293B', fontSize: '20px', fontWeight: '700' }}>
                {entries.length}
              </div>
              <div style={{ color: '#64748B', fontSize: '11px' }}>笔</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ color: '#64748B', fontSize: '12px', marginBottom: '4px' }}>最新分录时间</div>
              <div style={{ color: '#1E293B', fontSize: '14px', fontWeight: '600' }}>
                {entries.length > 0 && latestTime ? 
                  new Date(latestTime).toLocaleString('zh-CN', { 
                    month: '2-digit', 
                    day: '2-digit', 
                    hour: '2-digit', 
                    minute: '2-digit' 
                  }) : 
                  '暂无数据'
                }
              </div>
            </div>
          </div>
          <div style={{
            padding: '8px 12px',
            backgroundColor: '#DBEAFE',
            borderRadius: '6px',
            border: '1px solid #3B82F6'
          }}>
            <span style={{ color: '#1D4ED8', fontSize: '12px', fontWeight: '600' }}>
              🔒 分录不可变
            </span>
          </div>
        </div>
      </div>
    );
  }

  /**
   * 渲染合计行（包含分录总数、借贷合计和平衡状态）
   * @param entries 会计分录数组
   */
  static renderTotalRow(entries: any[]) {
    const totals = this.calculateTotals(entries);
    
    return (
      <div style={{
        marginTop: 16,
        padding: '16px 24px',
        backgroundColor: '#F8FAFC',
        borderRadius: '8px',
        border: '1px solid #E2E8F0'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', gap: '32px' }}>
            <div>
              <span style={{ color: '#64748B', fontSize: '14px', marginRight: '8px' }}>分录总数:</span>
              <span style={{ color: '#3B82F6', fontWeight: '600', fontSize: '16px' }}>
                {entries.length} 笔
              </span>
            </div>
            <div>
              <span style={{ color: '#64748B', fontSize: '14px', marginRight: '8px' }}>借方合计:</span>
              <span style={{ color: '#E31E24', fontWeight: '600', fontSize: '16px' }}>
                ¥{totals.debitTotal.toFixed(2)}
              </span>
            </div>
            <div>
              <span style={{ color: '#64748B', fontSize: '14px', marginRight: '8px' }}>贷方合计:</span>
              <span style={{ color: '#E31E24', fontWeight: '600', fontSize: '16px' }}>
                ¥{totals.creditTotal.toFixed(2)}
              </span>
            </div>
          </div>
          <div style={{
            padding: '6px 12px',
            borderRadius: '6px',
            backgroundColor: totals.isBalanced ? '#DCFCE7' : '#FEE2E2',
            border: `1px solid ${totals.isBalanced ? '#16A34A' : '#DC2626'}`
          }}>
            <span style={{
              color: totals.isBalanced ? '#16A34A' : '#DC2626',
              fontWeight: '600',
              fontSize: '14px'
            }}>
              {totals.isBalanced ? '✓ 借贷平衡' : '✗ 借贷不平衡'}
            </span>
          </div>
        </div>
      </div>
    );
  }

  /**
   * 获取支付操作时间戳列配置
   */
  static getPaymentTimestampColumn() {
    return {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>支付操作时间</span>,
      dataIndex: 'paymentTimestamp',
      key: 'paymentTimestamp',
      width: 160,
      render: (timestamp: string, record: any) => {
        // 如果没有支付时间戳，使用当前时间或记录创建时间
        const displayTime = timestamp || record.createdAt || new Date().toISOString();
        const formattedTime = new Date(displayTime).toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        });
        return (
          <span style={{ color: '#6B7280', fontSize: '12px' }}>{formattedTime}</span>
        );
      }
    };
  }

  /**
   * 获取分录状态列配置
   */
  static getEntryStatusColumn() {
    return {
      title: <span style={{ color: '#0F172A', fontWeight: '600', fontSize: '14px' }}>分录状态</span>,
      dataIndex: 'entryStatus',
      key: 'entryStatus',
      width: 120,
      render: (_status: string, _record: any) => {
        // 所有已生成的会计分录都是不可变的
        const isImmutable = true;
        const statusText = isImmutable ? '已确认' : '草稿';
        const statusColor = isImmutable ? '#16A34A' : '#F59E0B';
        const bgColor = isImmutable ? '#DCFCE7' : '#FEF3C7';
        
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span style={{
              display: 'inline-block',
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              backgroundColor: statusColor
            }}></span>
            <span style={{
              color: statusColor,
              fontSize: '12px',
              fontWeight: '600',
              padding: '2px 6px',
              borderRadius: '4px',
              backgroundColor: bgColor
            }}>
              {statusText}
            </span>
          </div>
        );
      }
    };
  }
}

export default JournalEntryImmutable;
