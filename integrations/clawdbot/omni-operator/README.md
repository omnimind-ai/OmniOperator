# OmniOperator Plugin

通过 HTTP API 控制 Android 手机的 Clawdbot 插件。配合 OmniOperator DevServer App 使用。

## 视频演示

<table>
  <tr>
    <td><a href="../../../1.mp4">演示 1（MP4）</a></td>
    <td><a href="../../../2.mp4">演示 2（MP4）</a></td>
  </tr>
</table>

## 前置要求

1. Android 手机上安装并运行 **OmniOperator DevServer** App
2. 手机和运行 Clawdbot 的电脑在同一局域网
3. 在手机上授予 OmniOperator 以下权限：
   - 无障碍服务
   - 屏幕录制/截图
   - 悬浮窗（在其他应用上层显示）

## 配置设备

### 1. 启用插件

```bash
clawdbot plugins enable omni-operator
```

### 2. 配置设备

```bash
# 添加设备（phone 是设备 ID，可自定义）
clawdbot config set plugins.entries.omni-operator.config.devices.phone.host "192.168.1.100"

# 端口（可选，默认 8080）
clawdbot config set plugins.entries.omni-operator.config.devices.phone.port 8080

# 设备名称（可选，用于显示）
clawdbot config set plugins.entries.omni-operator.config.devices.phone.label "我的手机"

# 设置默认设备
clawdbot config set plugins.entries.omni-operator.config.defaultDevice "phone"

# 超时时间（可选，默认 30000 毫秒）
clawdbot config set plugins.entries.omni-operator.config.timeout 30000
```

### 3. 重启 Gateway

```bash
clawdbot gateway restart
```

### 4. 验证连接

```bash
clawdbot agent --message "检查手机连接状态" --agent main
```

## 配置项说明

| 配置项 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| `devices.<id>.host` | 是 | - | 设备 IP 地址 |
| `devices.<id>.port` | 否 | 8080 | DevServer 端口 |
| `devices.<id>.label` | 否 | - | 设备显示名称 |
| `defaultDevice` | 否 | 第一个设备 | 默认使用的设备 ID |
| `timeout` | 否 | 30000 | 请求超时（毫秒） |

## 可用工具

### omni_status - 状态诊断
- `action="ping"` - 检查连接
- `action="devices"` - 列出设备

### omni_screenshot - 屏幕捕获
- `action="image"` - 截图
- `action="xml"` - UI 树结构
- `action="metadata"` - 当前应用信息

### omni_interact - 触摸交互
- `action="click", x, y` - 点击坐标
- `action="longClick", x, y` - 长按
- `action="scroll", startX, startY, endX, endY` - 滚动

### omni_input - 文本输入
- `action="inputText", nodeId, text` - 输入到节点
- `action="inputToFocused", text` - 输入到焦点
- `action="copyToClipboard", text` - 复制到剪贴板

### omni_app - 应用管理
- `action="launch", packageName` - 启动应用
- `action="list"` - 列出应用

### omni_navigate - 导航
- `action="home"` - 返回桌面
- `action="back"` - 返回

### omni_dialog - 对话框
- `action="showMessage", message` - 显示消息
- `action="confirm", message` - 确认框
- `action="choice", message, options` - 选择框

## 常见问题

**连接失败**：确认同一局域网、DevServer 运行中、IP 正确

**截图返回空**：检查屏幕录制权限

**XML 返回空**：检查无障碍服务，部分应用（如微信）限制访问
