import type { ThemeConfig } from 'antd';

export const theme: ThemeConfig = {
  token: {
    // 主色调
    // colorPrimary: '#b5120f',
    // 背景色
    colorBgBase: '#F5F5DC',
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
    },
    Table: {
        rowExpandedBg: '#bae0ff'
    }
  },
};
