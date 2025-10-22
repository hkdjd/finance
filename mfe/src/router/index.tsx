import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import PageA from '../pages/PageA';
import ContractDetail from '../pages/ContractDetail';
import Reports from '../pages/Reports';


const AppRouter: React.FC = () => {
  return (
    <Routes>
      {/* 首页重定向到文件上传页面 */}
      <Route path="/" element={<Navigate to="/page-a" replace />} />
      
      {/* 合同详情页（带 id 参数） */}
      <Route path="/contract/:id" element={<ContractDetail />} />
      
      {/* PageA 路由保留 */}
      <Route path="/page-a" element={<PageA />} />
      
      {/* 报表页面 */}
      <Route path="/reports" element={<Reports />} />
      
      {/* 404 重定向到首页 */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default AppRouter;
