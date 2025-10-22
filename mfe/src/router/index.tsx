import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Login from '../pages/Login';
import PageA from '../pages/PageA';
import ContractDetail from '../pages/ContractDetail';

// 登录状态检查组件
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';

  if (!isLoggedIn) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};

const AppRouter: React.FC = () => {
  return (
    <Routes>
      {/* 首页 - 登录页面（带背景图） */}
      <Route path="/" element={<Login />} />


      {/* 需要登录保护的页面 */}
      <Route
        path="/page-a"
        element={
          <ProtectedRoute>
            <PageA />
          </ProtectedRoute>
        }
      />

      <Route
        path="/contract/:id"
        element={
          <ProtectedRoute>
            <ContractDetail />
          </ProtectedRoute>
        }
      />

      {/* 404 重定向到首页 */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default AppRouter;
