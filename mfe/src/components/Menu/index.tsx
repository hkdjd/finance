import React from 'react';
import { Menu } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { UnorderedListOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { useNavigationGuard } from '../../contexts/NavigationGuardContext';

type MenuItem = Required<MenuProps>['items'][number];

const AppMenu: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { checkNavigation } = useNavigationGuard();

  // 菜单项配置
  const items: MenuItem[] = [
    {
      key: '/page-a',
      icon: <UnorderedListOutlined />,
      label: '合同列表',
    },
  ];

  // 根据当前路径确定选中的菜单项
  const selectedKey = location.pathname === '/page-a' ? '/page-a' : '';

  // 菜单点击处理
  const handleMenuClick: MenuProps['onClick'] = (e) => {
    checkNavigation(() => {
      navigate(e.key);
    });
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
