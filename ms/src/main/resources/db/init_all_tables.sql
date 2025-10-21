-- 财务系统数据库初始化脚本
-- 按照表依赖关系顺序执行建表脚本
-- 执行顺序: contracts -> amortization_entries -> payments -> journal_entries

-- 设置数据库编码和时区
SET client_encoding = 'UTF8';
SET timezone = 'Asia/Shanghai';

-- 开始事务
BEGIN;

-- 1. 创建合同表 (基础表，无外键依赖)
\i contracts_init.sql

-- 2. 创建摊销明细表 (依赖contracts表)
\i amortization_entries_init.sql

-- 3. 创建付款表 (依赖contracts表)
\i payments_init.sql

-- 4. 创建会计分录表 (依赖payments表)
\i journal_entries_init.sql

-- 提交事务
COMMIT;

-- 显示创建结果
SELECT 
    schemaname,
    tablename,
    tableowner
FROM pg_tables 
WHERE schemaname = 'public' 
    AND tablename IN ('contracts', 'amortization_entries', 'payments', 'journal_entries')
ORDER BY tablename;
