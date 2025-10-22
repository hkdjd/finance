-- 移除摊销期间和入账期间字段的长度限制
-- 将 VARCHAR(7) 改为 VARCHAR(255) 以支持任意长度的日期格式

-- PostgreSQL
ALTER TABLE amortization_entries 
    ALTER COLUMN amortization_period TYPE VARCHAR(255);

ALTER TABLE amortization_entries 
    ALTER COLUMN accounting_period TYPE VARCHAR(255);

-- H2 数据库（如果需要）
-- ALTER TABLE amortization_entries ALTER COLUMN amortization_period VARCHAR(255);
-- ALTER TABLE amortization_entries ALTER COLUMN accounting_period VARCHAR(255);
