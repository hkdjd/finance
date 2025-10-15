# 脚本文件说明

本文件夹包含项目的各种shell脚本，用于构建、运行和测试应用程序。

## 构建和运行脚本

### build-and-run.sh
- **功能**: 构建并运行应用程序
- **用法**: `./build-and-run.sh`
- **说明**: 执行Maven构建并启动Spring Boot应用

### run-local.sh
- **功能**: 本地运行应用程序
- **用法**: `./run-local.sh`
- **说明**: 直接启动本地开发环境

## API测试脚本

### test-apis.sh
- **功能**: 测试基础API接口
- **用法**: `./test-apis.sh`
- **说明**: 测试项目的核心API功能

### test-contract-upload.sh
- **功能**: 测试合同上传功能
- **用法**: `./test-contract-upload.sh`
- **说明**: 测试合同文件上传和解析功能

### test-new-features.sh
- **功能**: 测试新功能
- **用法**: `./test-new-features.sh`
- **说明**: 测试最新开发的功能特性

### test-selectedperiods-id.sh
- **功能**: 测试选中期间ID功能
- **用法**: `./test-selectedperiods-id.sh`
- **说明**: 测试期间选择相关的API

### test-simplified-api.sh
- **功能**: 测试简化API
- **用法**: `./test-simplified-api.sh`
- **说明**: 测试简化版本的API接口

### test-status-field.sh
- **功能**: 测试状态字段功能
- **用法**: `./test-status-field.sh`
- **说明**: 测试状态字段相关的功能

## 使用说明

1. 确保脚本具有执行权限：
   ```bash
   chmod +x scripts/*.sh
   ```

2. 从项目根目录运行脚本：
   ```bash
   ./scripts/脚本名称.sh
   ```

3. 或者进入scripts目录运行：
   ```bash
   cd scripts
   ./脚本名称.sh
   ```

## 注意事项

- 运行测试脚本前请确保应用程序已启动
- 某些脚本可能需要特定的环境配置
- 如果遇到权限问题，请使用 `chmod +x` 命令添加执行权限
