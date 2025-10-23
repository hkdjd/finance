import React from 'react';
import { Layout } from 'antd';
import { useLocation } from 'react-router-dom';
import AppHeader from '../Header';
import AppRouter from '../../../router';

const { Content } = Layout;

const AppLayout: React.FC = () => {
  const location = useLocation();
  const isLoginPage = location.pathname === '/' || location.pathname === '/login-alt';

  // 登录页面使用全屏样式
  const contentStyle: React.CSSProperties = isLoginPage
    ? {
        padding: 0,
        margin: 0,
        minHeight: '100vh',
        width: '100%',
        overflow: 'hidden',
      }
    : {
        backgroundColor: '#F5F5DC',
        minHeight: '75vh',
        padding: '20px',
        overflow: 'auto',
      };

  return (
    <Layout style={{ minHeight: '100vh', margin: 0, padding: 0 }}>
      {/* 登录页面不显示Header */}
      {!isLoginPage && <AppHeader />}
      <Content style={contentStyle}>
        <AppRouter />
      </Content>
    </Layout>
  );
};

export default AppLayout;
