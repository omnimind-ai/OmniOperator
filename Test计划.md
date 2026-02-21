# OpenOmniOperator 最基本测试补齐计划（Baseline）

## 目标
以最小投入建立可持续回归保护，优先覆盖高风险核心路径，避免后续改动引入隐性崩溃和协议回归。

## 优先级与范围

### 1. Android JVM 单元测试（P0，先补）
目录建议：`app/src/test/java/...`

#### DevServerManager
目标文件：`app/src/main/java/cn/com/omnimind/omnibot/DevServerManager.kt`

测试点：
1. 端口占用时自动递增重试逻辑。
2. `isAddressInUseError` 对不同异常链识别正确。
3. `startServer`/`stopServer` 状态切换与幂等行为。

#### OmniDevServer 参数校验与鉴权
目标文件：`app/src/main/java/cn/com/omnimind/omnibot/OmniDevServer.kt`

测试点：
1. 缺失必要参数时返回 400（如 `clickNode`、`inputText`）。
2. 启用 `apiKey` 后无/错 `Authorization` 返回 401。
3. `/health` 在启用鉴权时仍可访问。

#### ImageUtils
目标文件：`app/src/main/java/cn/com/omnimind/omnibot/controller/screenshot/OmniScreenshotController.kt`（内含 `ImageUtils`）

测试点：
1. 质量参数边界（1/100）生效。
2. 缩放开关与缩放比例（1-100）生效。

#### XmlTreeUtils（先做纯逻辑）
目标文件：`app/src/main/java/cn/com/omnimind/omnibot/controller/screenshot/OmniScreenshotController.kt`

测试点：
1. `sanitizeXmlString` 能清洗非法 XML 字符。
2. `serializeXml` 输出包含关键节点属性。

### 2. Android 仪器化测试（P0，少量关键烟雾）
目录建议：`app/src/androidTest/java/...`

#### Activity 启动烟雾测试
目标文件：`app/src/main/java/cn/com/omnimind/omnibot/MainActivity.kt`

测试点：
1. `MainActivity` 可启动且不崩溃。

#### 权限相关可自动化分支
目标文件：`app/src/main/java/cn/com/omnimind/omnibot/MainActivity.kt`、`app/src/main/java/cn/com/omnimind/omnibot/OmniOperatorService.kt`

测试点：
1. 无障碍状态查询分支可调用。
2. 电池优化状态查询分支可调用。

### 3. Flutter 测试（P1，投入小收益高）
目录建议：`flutter_module/test/...`

#### history_service
目标文件：`flutter_module/lib/services/history_service.dart`

测试点：
1. 去重逻辑正确。
2. 新消息置顶。
3. 最多保留 3 条。
4. 空字符串不入库。

#### settings_page 参数边界
目标文件：`flutter_module/lib/pages/settings_page.dart`

测试点：
1. 截图质量值 clamp 到 1-100。
2. 缩放值 clamp 到 1-100。
3. 开关变化后触发保存与平台通道调用（MethodChannel mock）。

#### home_page 基本交互
目标文件：`flutter_module/lib/pages/home_page.dart`

测试点：
1. 空消息发送被拦截。
2. DevServer 运行时移动端发送被拦截并提示。

### 4. Clawdbot 集成插件测试（P1）
目录建议：`integrations/clawdbot/omni-operator/test/...`

#### client.ts
目标文件：`integrations/clawdbot/omni-operator/src/client.ts`

测试点：
1. `buildUrl` 拼接正确。
2. 超时返回统一失败结构。
3. 非 2xx 响应转换为失败结构。
4. `formatImageResult` 对 data URI 与 MIME 识别正确。

## 第一阶段最小交付规模（建议一周完成）
1. Android JVM：8-12 个用例。
2. Android Instrumented：2-4 个烟雾用例。
3. Flutter：8-10 个用例。
4. TypeScript 插件：6-8 个用例。

总计建议：25-35 个基础用例。

## 验收标准（Baseline）
1. CI 至少新增并通过：Android 单测、Flutter test、TS test 三类任务。
2. P0 清单中的测试全部落地且稳定通过。
3. 任一核心回归（鉴权、参数校验、消息历史、URL 构造）可被自动测试捕获。
