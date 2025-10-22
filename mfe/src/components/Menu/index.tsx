import React from 'react';
import { Menu } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { UnorderedListOutlined, BarChartOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';

type MenuItem = Required<MenuProps>['items'][number];

const AppMenu: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  // 菜单项配置
  const items: MenuItem[] = [
    {
      key: '/page-a',
      icon: <UnorderedListOutlined />,
      label: '合同列表',
    },
    {
      key: '/reports',
      icon: <BarChartOutlined />,
      label: '财务报表',
    },
  ];

  // 根据当前路径确定选中的菜单项
  const selectedKey = location.pathname === '/page-a' ? '/page-a' : location.pathname === '/reports' ? '/reports' : '';

  // 菜单点击处理
  const handleMenuClick: MenuProps['onClick'] = (e) => {
    navigate(e.key);
  };

  return (
    <Menu
      mode="horizontal"
      selectedKeys={[selectedKey]}
      items={items}
      onClick={handleMenuClick}
      style={{
        flex: 1,
        minWidth: 0,
        backgroundColor: 'transparent',
        borderBottom: 'none'
      }}
    />
  );
};

export default AppMenu;
