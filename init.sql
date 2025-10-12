-- 初始化PostgreSQL数据库脚本
-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS finance2;

-- 使用数据库
\c finance2;

-- 创建用户（如果不存在）
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'finance2_user') THEN

      CREATE ROLE finance2_user LOGIN PASSWORD 'finance2_password';
   END IF;
END
$do$;

-- 授予权限
GRANT ALL PRIVILEGES ON DATABASE finance2 TO finance2_user;
GRANT ALL ON SCHEMA public TO finance2_user;
