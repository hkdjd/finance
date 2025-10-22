import React from 'react';
import { Layout, Typography, Button, Dropdown, Avatar, Space, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { UserOutlined, LogoutOutlined } from '@ant-design/icons';
import AppMenu from '../../Menu';
import { useNavigationGuard } from '../../../contexts/NavigationGuardContext';

const { Header } = Layout;
const { Text } = Typography;

const AppHeader: React.FC = () => {
  const navigate = useNavigate();

  // 获取用户信息
  const username = localStorage.getItem('username') || '用户';
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';

  const { checkNavigation } = useNavigationGuard();
  
  const headerStyle: React.CSSProperties = {
    backgroundColor: 'rgb(235 235 230 / 50%)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 20px',
    width: '100%',
    zIndex: 1000,
  };

  // 登出处理
  const handleLogout = () => {
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('username');
    message.success('已成功登出');
    navigate('/');
  };

  // 用户菜单配置
  const userMenuItems = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '登出',
      onClick: handleLogout,
    },
  ];

  return (
    <Header style={headerStyle}>
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <img
          src="/OCBC.png"
          alt="OCBC Logo"
          style={{
            height: '32px',
            width: 'auto',
            cursor: 'pointer',
            marginRight: '40px'
          }}
          onClick={() => navigate('/')}
        />
        {isLoggedIn && <AppMenu />}
      </div>

      {isLoggedIn && (
        <Dropdown
          menu={{ items: userMenuItems }}
          placement="bottomRight"
          trigger={['click']}
        >
          <Button
            type="text"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              height: 'auto',
              padding: '8px 12px',
            }}
          >
            <Avatar size="small" icon={<UserOutlined />} />
            <Space>
              <Text style={{ color: '#262626' }}>{username}</Text>
            </Space>
          </Button>
        </Dropdown>
      )}
    </Header>
  );
};

export default AppHeader;
