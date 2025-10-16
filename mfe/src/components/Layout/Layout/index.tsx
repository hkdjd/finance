import React from 'react';
import { Layout } from 'antd';
import AppHeader from '../Header';
import AppRouter from '../../../router';

const { Content } = Layout;

const AppLayout: React.FC = () => {
  const contentStyle: React.CSSProperties = {
    backgroundColor: '#F5F5DC',
    minHeight: '75vh',
    padding: '20px',
    overflow: 'auto',
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <AppHeader />
      <Content style={contentStyle}>
        <AppRouter />
      </Content>
    </Layout>
  );
};

export default AppLayout;
