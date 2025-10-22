# 变更日志 - userId默认值和批量关键字设置优化

**日期**: 2025-10-22  
**模块**: ms (后端业务处理模块)  
**影响范围**: 合同上传、自定义关键字管理

---

## 变更概述

本次变更主要包含两个功能优化：
1. **添加默认userId逻辑**: 所有涉及userId的接口，当未传入userId时，自动使用默认的admin用户ID（值为1）
2. **优化批量关键字设置**: 批量设置关键字接口改为"先删除后插入"模式，实现完全替换用户的关键字配置

---

## 详细变更

### 1. 新增常量类

**文件**: `src/main/java/com/ocbc/finance/constants/UserConstants.java`

```java
public class UserConstants {
    /**
     * 默认管理员用户ID
     * 当接口未传入userId时，使用此默认值
     */
    public static final Long DEFAULT_ADMIN_USER_ID = 1L;
}
```

---

### 2. 修改Controller层

#### 2.1 CustomKeywordController

**文件**: `src/main/java/com/ocbc/finance/controller/CustomKeywordController.java`

**变更内容**:
- 所有接口方法中添加userId默认值逻辑
- 将userId参数改为可选（`required = false`）
- 当userId为null时，自动设置为`UserConstants.DEFAULT_ADMIN_USER_ID`

**影响的接口**:
- `POST /api/custom-keywords` - 创建关键字
- `GET /api/custom-keywords/user/{userId}` - 获取用户关键字列表
- `GET /api/custom-keywords/user/{userId}/strings` - 获取关键字字符串列表
- `PUT /api/custom-keywords/{id}` - 更新关键字
- `DELETE /api/custom-keywords/{id}` - 删除关键字
- `POST /api/custom-keywords/batch` - 批量设置关键字

#### 2.2 ContractUploadController

**文件**: `src/main/java/com/ocbc/finance/controller/ContractUploadController.java`

**变更内容**:
- `uploadContract`方法中添加userId默认值逻辑
- 当userId为null时，自动设置为`UserConstants.DEFAULT_ADMIN_USER_ID`

**影响的接口**:
- `POST /contracts/upload` - 合同上传

---

### 3. 修改Service层

#### 3.1 CustomKeywordService

**文件**: `src/main/java/com/ocbc/finance/service/CustomKeywordService.java`

**方法**: `batchCreateKeywords`

**变更前**:
```java
// 过滤已存在的关键字，仅创建不存在的关键字
List<CustomKeyword> customKeywords = keywords.stream()
    .filter(keyword -> {
        CustomKeyword existing = customKeywordRepository.findByUserIdAndKeyword(userId, keyword);
        return existing == null;
    })
    .map(...)
    .collect(Collectors.toList());
```

**变更后**:
```java
// 1. 先删除该用户的所有关键字
customKeywordRepository.deleteByUserId(userId);
log.info("已删除用户{}的所有关键字", userId);

// 2. 批量插入新的关键字
List<CustomKeyword> customKeywords = keywords.stream()
    .map(...)
    .collect(Collectors.toList());
```

**业务逻辑变更**:
- **旧逻辑**: 过滤已存在的关键字，仅插入新的关键字（增量更新）
- **新逻辑**: 先删除用户所有关键字，再批量插入（完全替换）

---

### 4. 文档更新

#### 4.1 自定义关键字管理文档

**文件**: `docs/18-custom-keywords-manage自定义关键字管理/README.md`

**更新内容**:
- 所有接口的userId参数说明改为"否"（非必填）
- 添加默认值说明："未传入时默认使用admin用户ID=1"
- 更新批量设置接口的业务规则说明
- 在注意事项中新增默认用户ID和批量设置逻辑的说明

#### 4.2 合同上传文档

**文件**: `docs/03-contracts-upload合同上传/README.md`

**更新内容**:
- userId参数说明添加默认值说明
- 在注意事项中新增默认用户ID的说明

---

## 影响分析

### 向后兼容性

✅ **完全兼容** - 本次变更对现有调用方完全向后兼容：
- 原本传入userId的调用方式不受影响
- 原本不传userId的调用，现在会自动使用默认admin用户ID

### 批量设置行为变更

⚠️ **行为变更** - 批量设置关键字接口的行为发生变化：
- **旧行为**: 增量添加，不删除已有关键字
- **新行为**: 完全替换，先删除所有再插入

**影响**: 如果前端或其他调用方依赖增量添加的行为，需要调整调用逻辑。

---

## 测试建议

### 1. 默认userId功能测试

```bash
# 测试1: 不传userId创建关键字
curl -X POST http://localhost:8081/api/custom-keywords \
  -H 'Content-Type: application/json' \
  -d '{"keyword": "测试关键字"}'

# 测试2: 不传userId上传合同
curl -X POST http://localhost:8081/contracts/upload \
  -F 'file=@contract.pdf'

# 测试3: 不传userId查询关键字
curl http://localhost:8081/api/custom-keywords/user/1
```

### 2. 批量设置功能测试

```bash
# 测试1: 创建初始关键字
curl -X POST http://localhost:8081/api/custom-keywords/batch?userId=1 \
  -H 'Content-Type: application/json' \
  -d '["关键字1", "关键字2", "关键字3"]'

# 测试2: 批量替换（应该删除旧的，只保留新的）
curl -X POST http://localhost:8081/api/custom-keywords/batch?userId=1 \
  -H 'Content-Type: application/json' \
  -d '["关键字4", "关键字5"]'

# 测试3: 验证结果（应该只有关键字4和关键字5）
curl http://localhost:8081/api/custom-keywords/user/1
```

---

## 部署注意事项

1. **数据库**: 无需数据库迁移，使用现有表结构
2. **配置**: 无需修改配置文件
3. **依赖**: 无新增外部依赖
4. **重启**: 需要重启ms模块服务以加载新代码

---

## 相关文件清单

### 新增文件
- `src/main/java/com/ocbc/finance/constants/UserConstants.java`

### 修改文件
- `src/main/java/com/ocbc/finance/controller/CustomKeywordController.java`
- `src/main/java/com/ocbc/finance/controller/ContractUploadController.java`
- `src/main/java/com/ocbc/finance/service/CustomKeywordService.java`
- `docs/18-custom-keywords-manage自定义关键字管理/README.md`
- `docs/03-contracts-upload合同上传/README.md`

---

## 回滚方案

如需回滚，执行以下步骤：
1. 恢复上述修改的文件到变更前的版本
2. 删除新增的`UserConstants.java`文件
3. 重启ms模块服务

---

## 联系人

如有问题，请联系开发团队。
