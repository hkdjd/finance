import React from 'react';
import { Layout, Typography, Menu } from 'antd';
import { useNavigate } from 'react-router-dom';
import AppMenu from '../../Menu';

const { Header } = Layout;
const { Title } = Typography;

const AppHeader: React.FC = () => {
  const navigate = useNavigate();
  
  const headerStyle: React.CSSProperties = {
    backgroundColor: 'rgb(235 235 230 / 50%)',
    display: 'flex',
    alignItems: 'center',
    padding: '0 20px',
    width: '100%',
    zIndex: 1000,
  };

  const titleStyle: React.CSSProperties = {
    color: '#b5120f',
    margin: 0,
    marginRight: '40px',
    fontWeight: 'bold',
    cursor: 'pointer'
  };

  return (
    <Header style={headerStyle}>
      <Title 
        level={2} 
        style={titleStyle}
        onClick={() => navigate('/')}
      >
        OCBC
      </Title>
      <AppMenu />
    </Header>
  );
};

export default AppHeader;
