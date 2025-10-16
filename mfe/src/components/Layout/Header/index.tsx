import React from 'react';
import { Layout, Typography } from 'antd';

const { Header } = Layout;
const { Title } = Typography;

const AppHeader: React.FC = () => {
  const headerStyle: React.CSSProperties = {
    backgroundColor: '#F5F5DC',
    display: 'flex',
    alignItems: 'center',
    paddingLeft: '20px',
    width: '100%',
    zIndex: 1000,
  };

  return (
    <Header style={headerStyle}>
      <Title 
        level={2} 
        style={{ 
          color: '#b5120f', 
          margin: 0,
          fontWeight: 'bold'
        }}
      >
        OCBC
      </Title>
    </Header>
  );
};

export default AppHeader;
