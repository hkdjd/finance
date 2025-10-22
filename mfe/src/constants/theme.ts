import type { ThemeConfig } from 'antd';

export const theme: ThemeConfig = {
  token: {
    // 主色调
    colorPrimary: '#b5120f',
    // 背景色 - 改为白色，这样所有组件默认背景都是白色
    colorBgBase: '#ffffff',
    colorIcon: '#b5120f',

    // 圆角
    borderRadius: 6,
    // 字体
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif',
    // 禁用线框模式
    wireframe: false,
  },
  components: {
    Layout: {
      headerBg: '#F5F5DC',
      bodyBg: '#F5F5DC',
    },
    Button: {
      primaryColor: '#ffffff',
      defaultBg: '#ffffff', // 默认按钮背景色
      defaultBorderColor: '#d9d9d9', // 默认按钮边框色
    },
    Input: {
      colorBgContainer: '#ffffff', // Input 背景色
    },
    InputNumber: {
      colorBgContainer: '#ffffff', // InputNumber 背景色
    },
    DatePicker: {
      colorBgContainer: '#ffffff', // DatePicker 背景色
    },
    Select: {
      colorBgContainer: '#ffffff', // Select 背景色
    },
    Table: {
      rowExpandedBg: '#bae0ff',
      colorBgContainer: '#ffffff', // Table 背景色
    },
    Menu: {
      itemColor: '#b5120f'
    }
  },
};
