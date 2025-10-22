import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import AppLayout from './components/Layout/Layout';
import { theme } from './constants/theme';
import { NavigationGuardProvider } from './contexts/NavigationGuardContext';

const App: React.FC = () => {
  return (
    <ConfigProvider theme={theme}>
      <BrowserRouter>
        <NavigationGuardProvider>
          <AppLayout />
        </NavigationGuardProvider>
      </BrowserRouter>
    </ConfigProvider>
  );
};

export default App;
