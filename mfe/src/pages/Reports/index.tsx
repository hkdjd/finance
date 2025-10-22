import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Spin, message, Statistic } from 'antd';
import {
  BarChartOutlined,
  PieChartOutlined,
  FileTextOutlined,
  DollarOutlined,
  WalletOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { getDashboardReport, getVendorDistribution } from '../../api';
import type {
  DashboardReportResponse,
  VendorDistributionResponse,
} from '../../api/reports/types';
import styles from './styles.module.css';

const Reports: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [dashboardData, setDashboardData] = useState<DashboardReportResponse | null>(null);
  const [vendorData, setVendorData] = useState<VendorDistributionResponse | null>(null);

  // 加载报表数据
  const loadReportData = async () => {
    setLoading(true);
    try {
      const [dashboard, vendor] = await Promise.all([
        getDashboardReport(),
        getVendorDistribution(),
      ]);
      setDashboardData(dashboard as any);
      setVendorData(vendor as any);
    } catch (error) {
      message.error('加载报表数据失败');
      console.error('加载报表数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReportData();
  }, []);

  // 柱状图配置
  const getBarChartOption = () => {
    if (!dashboardData) return {};

    return {
      title: {
        text: `财务仪表盘 - ${dashboardData.statisticsMonth}`,
        left: 'center',
        textStyle: {
          fontSize: 18,
          fontWeight: 'bold',
        },
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow',
        },
        formatter: (params: any) => {
          const item = params[0];
          const name = item.name;
          const value = item.value;
          
          if (name === '生效合同数量') {
            return `${name}<br/>${value} 个`;
          } else {
            return `${name}<br/>¥${value.toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`;
          }
        },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: ['生效合同数量', '本月摊销金额', '剩余待付款金额'],
        axisLabel: {
          interval: 0,
          rotate: 0,
          fontSize: 12,
        },
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: (value: number) => {
            if (value >= 10000) {
              return `${(value / 10000).toFixed(1)}万`;
            }
            return value.toString();
          },
        },
      },
      series: [
        {
          name: '数值',
          type: 'bar',
          data: [
            dashboardData.activeContractCount,
            dashboardData.currentMonthAmortization,
            dashboardData.remainingPayableAmount,
          ],
          itemStyle: {
            color: (params: any) => {
              const colors = ['#5470c6', '#91cc75', '#fac858'];
              return colors[params.dataIndex];
            },
          },
          label: {
            show: true,
            position: 'top',
            formatter: (params: any) => {
              if (params.dataIndex === 0) {
                return params.value;
              } else {
                return `¥${params.value.toLocaleString('zh-CN')}`;
              }
            },
          },
          barWidth: '40%',
        },
      ],
    };
  };

  // 饼图配置
  const getPieChartOption = () => {
    if (!vendorData) return {};

    return {
      title: {
        text: '供应商分布',
        subtext: `合同总数: ${vendorData.totalContracts}`,
        left: 'center',
        textStyle: {
          fontSize: 18,
          fontWeight: 'bold',
        },
      },
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)',
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        top: 'middle',
        data: vendorData.vendors.map((v) => v.vendorName),
      },
      series: [
        {
          name: '合同数量',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['60%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2,
          },
          label: {
            show: true,
            formatter: '{b}: {d}%',
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 14,
              fontWeight: 'bold',
            },
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
          },
          data: vendorData.vendors.map((v) => ({
            value: v.contractCount,
            name: v.vendorName,
          })),
        },
      ],
    };
  };

  return (
    <div className={styles.reportsContainer}>
      <div className={styles.header}>
        <h1>
          <BarChartOutlined /> 财务报表
        </h1>
        <p>实时数据统计与分析</p>
      </div>

      <Spin spinning={loading}>
        {/* 数据卡片 */}
        {dashboardData && (
          <Row gutter={[16, 16]} className={styles.statsRow}>
            <Col xs={24} sm={8}>
              <Card bordered={false} className={styles.statCard}>
                <Statistic
                  title="生效合同数量"
                  value={dashboardData.activeContractCount}
                  prefix={<FileTextOutlined />}
                  suffix="个"
                  valueStyle={{ color: '#5470c6' }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={8}>
              <Card bordered={false} className={styles.statCard}>
                <Statistic
                  title="本月摊销金额"
                  value={dashboardData.currentMonthAmortization}
                  prefix={<DollarOutlined />}
                  precision={2}
                  valueStyle={{ color: '#91cc75' }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={8}>
              <Card bordered={false} className={styles.statCard}>
                <Statistic
                  title="剩余待付款金额"
                  value={dashboardData.remainingPayableAmount}
                  prefix={<WalletOutlined />}
                  precision={2}
                  valueStyle={{ color: '#fac858' }}
                />
              </Card>
            </Col>
          </Row>
        )}

        {/* 图表区域 */}
        <Row gutter={[16, 16]} className={styles.chartsRow}>
          {/* 柱状图 */}
          <Col xs={24} lg={12}>
            <Card
              title={
                <span>
                  <BarChartOutlined /> 财务指标概览
                </span>
              }
              bordered={false}
              className={styles.chartCard}
            >
              {dashboardData ? (
                <ReactECharts
                  option={getBarChartOption()}
                  style={{ height: '400px' }}
                  notMerge={true}
                  lazyUpdate={true}
                />
              ) : (
                <div className={styles.emptyChart}>暂无数据</div>
              )}
            </Card>
          </Col>

          {/* 饼图 */}
          <Col xs={24} lg={12}>
            <Card
              title={
                <span>
                  <PieChartOutlined /> 供应商分布
                </span>
              }
              bordered={false}
              className={styles.chartCard}
            >
              {vendorData ? (
                <ReactECharts
                  option={getPieChartOption()}
                  style={{ height: '400px' }}
                  notMerge={true}
                  lazyUpdate={true}
                />
              ) : (
                <div className={styles.emptyChart}>暂无数据</div>
              )}
            </Card>
          </Col>
        </Row>

        {/* 供应商详细列表 */}
        {vendorData && vendorData.vendors.length > 0 && (
          <Row gutter={[16, 16]} className={styles.tableRow}>
            <Col span={24}>
              <Card title="供应商详细数据" bordered={false}>
                <table className={styles.vendorTable}>
                  <thead>
                    <tr>
                      <th>排名</th>
                      <th>供应商名称</th>
                      <th>合同数量</th>
                      <th>占比</th>
                    </tr>
                  </thead>
                  <tbody>
                    {vendorData.vendors.map((vendor, index) => (
                      <tr key={vendor.vendorName}>
                        <td>
                          <span className={styles.rank}>{index + 1}</span>
                        </td>
                        <td>{vendor.vendorName}</td>
                        <td>{vendor.contractCount} 个</td>
                        <td>
                          <span className={styles.percentage}>
                            {vendor.percentage.toFixed(2)}%
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr>
                      <td colSpan={2}>
                        <strong>合计</strong>
                      </td>
                      <td>
                        <strong>{vendorData.totalContracts} 个</strong>
                      </td>
                      <td>
                        <strong>100%</strong>
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </Card>
            </Col>
          </Row>
        )}
      </Spin>
    </div>
  );
};

export default Reports;
