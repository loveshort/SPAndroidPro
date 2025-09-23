# WebSocket 封装使用说明

这是一个完整的 WebSocket 连接管理封装，提供了连接、断开、重连、心跳检测等功能。

## 主要特性

- ✅ 自动重连机制
- ✅ 心跳检测
- ✅ 连接状态管理
- ✅ 消息发送和接收
- ✅ 线程安全
- ✅ 资源自动释放
- ✅ 可配置参数

## 核心类说明

### 1. WebSocketManager

主要的 WebSocket 连接管理类，提供所有核心功能。

### 2. WebSocketListener

状态监听器接口，用于接收连接状态变化和消息通知。

### 3. WebSocketUtils

工具类，提供消息格式化和常用操作。

### 4. WebSocketExample

使用示例类，展示如何使用 WebSocket 管理器。

## 快速开始

### 1. 基本使用

```java
// 创建 WebSocket 管理器
WebSocketManager manager = new WebSocketManager();

// 设置服务器地址
manager.setServerUrl("ws://your-websocket-server.com");

// 设置监听器
manager.setWebSocketListener(new WebSocketListener() {
    @Override
    public void onConnected() {
        Log.d("WebSocket", "连接成功");
    }

    @Override
    public void onMessageReceived(String message) {
        Log.d("WebSocket", "收到消息: " + message);
    }

    @Override
    public void onDisconnected(int code, String reason) {
        Log.d("WebSocket", "连接断开: " + reason);
    }

    // ... 其他回调方法
});

// 连接
manager.connect();

// 发送消息
manager.sendMessage("Hello WebSocket!");

// 断开连接
manager.disconnect();
```

### 2. 配置参数

```java
WebSocketManager manager = new WebSocketManager();

// 设置重连参数
manager.setReconnectInterval(3000);        // 重连间隔 3秒
manager.setMaxReconnectAttempts(5);        // 最大重连次数 5次

// 设置心跳参数
manager.setHeartbeatInterval(30000);       // 心跳间隔 30秒
manager.setHeartbeatMessage("ping");       // 心跳消息

// 设置服务器地址
manager.setServerUrl("ws://echo.websocket.org");
```

### 3. 使用工具类

```java
// 创建不同类型的消息
String heartbeatMsg = WebSocketUtils.createHeartbeatMessage();
String textMsg = WebSocketUtils.createTextMessage("Hello");
String errorMsg = WebSocketUtils.createErrorMessage("连接失败");

// 解析消息
WebSocketUtils.WebSocketMessage msg = WebSocketUtils.parseMessage(receivedMessage);
String type = msg.getType();
String content = msg.getContent();

// 格式化URL
String url = WebSocketUtils.formatWebSocketUrl("localhost", 8080, "/websocket");
```

## 连接状态

WebSocket 连接有以下几种状态：

- `DISCONNECTED`: 未连接
- `CONNECTING`: 连接中
- `CONNECTED`: 已连接
- `RECONNECTING`: 重连中

```java
// 检查连接状态
if (manager.isConnected()) {
    // 已连接，可以发送消息
    manager.sendMessage("Hello");
}

// 获取详细状态
WebSocketManager.ConnectionState state = manager.getConnectionState();
```

## 监听器回调

WebSocketListener 接口提供以下回调方法：

- `onConnected()`: 连接成功
- `onConnectionFailed(String error)`: 连接失败
- `onDisconnected(int code, String reason)`: 连接断开
- `onMessageReceived(String message)`: 收到消息
- `onSendFailed(String error)`: 发送失败
- `onReconnecting(int attempt)`: 开始重连
- `onReconnected()`: 重连成功
- `onReconnectFailed(String error)`: 重连失败

## 最佳实践

### 1. 生命周期管理

```java
public class MyActivity extends AppCompatActivity {
    private WebSocketManager webSocketManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化 WebSocket
        webSocketManager = new WebSocketManager();
        webSocketManager.setServerUrl("ws://your-server.com");
        webSocketManager.setWebSocketListener(listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 恢复连接
        webSocketManager.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停时断开连接
        webSocketManager.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放资源
        webSocketManager.release();
    }
}
```

### 2. 消息处理

```java
@Override
public void onMessageReceived(String message) {
    // 解析消息
    WebSocketUtils.WebSocketMessage msg = WebSocketUtils.parseMessage(message);

    // 根据消息类型处理
    switch (msg.getType()) {
        case WebSocketUtils.MessageType.HEARTBEAT:
            // 心跳消息，不需要处理
            break;
        case WebSocketUtils.MessageType.TEXT:
            // 文本消息
            handleTextMessage(msg.getContent());
            break;
        case WebSocketUtils.MessageType.ERROR:
            // 错误消息
            handleErrorMessage(msg.getContent());
            break;
        default:
            // 其他类型消息
            break;
    }
}
```

### 3. 错误处理

```java
@Override
public void onConnectionFailed(String error) {
    Log.e("WebSocket", "连接失败: " + error);
    // 显示错误提示给用户
    showErrorDialog("连接失败: " + error);
}

@Override
public void onReconnectFailed(String error) {
    Log.e("WebSocket", "重连失败: " + error);
    // 重连失败，可能需要用户手动重试
    showRetryDialog();
}
```

## 注意事项

1. **线程安全**: 所有回调都在主线程中执行，可以直接更新 UI
2. **资源释放**: 在 Activity/Fragment 销毁时调用 `release()` 方法释放资源
3. **网络权限**: 确保在 AndroidManifest.xml 中添加网络权限
4. **服务器兼容**: 确保 WebSocket 服务器支持标准的 WebSocket 协议

## 依赖

在 `build.gradle` 中添加以下依赖：

```gradle
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

## 许可证

此代码仅供学习和参考使用。

