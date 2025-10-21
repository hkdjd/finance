-- 数据库表结构验证脚本
-- 用于验证建表脚本执行结果

-- 1. 检查所有表是否创建成功
SELECT 
    '表创建检查' as check_type,
    tablename,
    CASE WHEN tablename IS NOT NULL THEN '✅ 存在' ELSE '❌ 不存在' END as status
FROM (
    VALUES 
        ('contracts'),
        ('amortization_entries'), 
        ('payments'),
        ('journal_entries')
) AS expected_tables(tablename)
LEFT JOIN pg_tables pt ON pt.tablename = expected_tables.tablename AND pt.schemaname = 'public'
ORDER BY expected_tables.tablename;

-- 2. 检查外键约束
SELECT 
    '外键约束检查' as check_type,
    conname as constraint_name,
    conrelid::regclass as table_name,
    confrelid::regclass as referenced_table,
    '✅ 正常' as status
FROM pg_constraint 
WHERE contype = 'f' 
    AND conrelid::regclass::text IN ('amortization_entries', 'payments', 'journal_entries')
ORDER BY conrelid::regclass;

-- 3. 检查主要索引
SELECT 
    '索引检查' as check_type,
    schemaname,
    tablename,
    indexname,
    '✅ 存在' as status
FROM pg_indexes 
WHERE schemaname = 'public' 
    AND tablename IN ('contracts', 'amortization_entries', 'payments', 'journal_entries')
    AND indexname LIKE 'idx_%'
ORDER BY tablename, indexname;

-- 4. 检查表字段
SELECT 
    '字段检查' as check_type,
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default,
    '✅ 正常' as status
FROM information_schema.columns 
WHERE table_schema = 'public' 
    AND table_name IN ('contracts', 'amortization_entries', 'payments', 'journal_entries')
ORDER BY table_name, ordinal_position;

-- 5. 检查检查约束
SELECT 
    '检查约束' as check_type,
    conname as constraint_name,
    conrelid::regclass as table_name,
    pg_get_constraintdef(oid) as constraint_definition,
    '✅ 正常' as status
FROM pg_constraint 
WHERE contype = 'c' 
    AND conrelid::regclass::text IN ('payments', 'journal_entries')
ORDER BY conrelid::regclass;

-- 6. 表统计信息
SELECT 
    '表统计' as check_type,
    schemaname,
    tablename,
    n_tup_ins as inserted_rows,
    n_tup_upd as updated_rows,
    n_tup_del as deleted_rows,
    '📊 统计' as status
FROM pg_stat_user_tables 
WHERE schemaname = 'public' 
    AND relname IN ('contracts', 'amortization_entries', 'payments', 'journal_entries')
ORDER BY relname;
