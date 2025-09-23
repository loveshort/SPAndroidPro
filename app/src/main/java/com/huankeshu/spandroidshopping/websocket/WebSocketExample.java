package com.huankeshu.spandroidshopping.websocket;

import android.util.Log;

/**
 * WebSocket 使用示例
 * 展示如何使用 WebSocketManager 进行连接、发送消息等操作
 */
public class WebSocketExample {
    
    private static final String TAG = "WebSocketExample";
    private WebSocketManager webSocketManager;
    
    public WebSocketExample() {
        initWebSocket();
    }
    
    /**
     * 初始化 WebSocket
     */
    private void initWebSocket() {
        // 创建 WebSocket 管理器
        webSocketManager = new WebSocketManager();
        
        // 设置服务器地址（请替换为实际的WebSocket服务器地址）
        webSocketManager.setServerUrl("ws://echo.websocket.org");
        
        // 配置重连参数
        webSocketManager.setReconnectInterval(3000); // 3秒重连间隔
        webSocketManager.setMaxReconnectAttempts(5); // 最大重连5次
        webSocketManager.setHeartbeatInterval(30000); // 30秒心跳间隔
        webSocketManager.setHeartbeatMessage("ping"); // 心跳消息
        
        // 设置监听器
        webSocketManager.setWebSocketListener(new WebSocketListener() {
            @Override
            public void onConnected() {
                Log.d(TAG, "WebSocket 连接成功");
                // 连接成功后可以发送消息
                sendTestMessage();
            }
            
            @Override
            public void onConnectionFailed(String error) {
                Log.e(TAG, "WebSocket 连接失败: " + error);
            }
            
            @Override
            public void onDisconnected(int code, String reason) {
                Log.d(TAG, "WebSocket 连接断开: " + code + " - " + reason);
            }
            
            @Override
            public void onMessageReceived(String message) {
                Log.d(TAG, "收到消息: " + message);
                // 处理接收到的消息
                handleMessage(message);
            }
            
            @Override
            public void onSendFailed(String error) {
                Log.e(TAG, "发送消息失败: " + error);
            }
            
            @Override
            public void onReconnecting(int attempt) {
                Log.d(TAG, "正在重连，第 " + attempt + " 次");
            }
            
            @Override
            public void onReconnected() {
                Log.d(TAG, "重连成功");
            }
            
            @Override
            public void onReconnectFailed(String error) {
                Log.e(TAG, "重连失败: " + error);
            }
        });
    }
    
    /**
     * 连接 WebSocket
     */
    public void connect() {
        if (webSocketManager != null) {
            webSocketManager.connect();
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        if (webSocketManager != null) {
            webSocketManager.disconnect();
        }
    }
    
    /**
     * 发送测试消息
     */
    private void sendTestMessage() {
        if (webSocketManager != null && webSocketManager.isConnected()) {
            String message = "Hello WebSocket!";
            boolean success = webSocketManager.sendMessage(message);
            Log.d(TAG, "发送消息结果: " + success);
        }
    }
    
    /**
     * 发送自定义消息
     */
    public void sendMessage(String message) {
        if (webSocketManager != null) {
            webSocketManager.sendMessage(message);
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private void handleMessage(String message) {
        // 根据消息内容进行不同的处理
        if ("ping".equals(message) || "pong".equals(message)) {
            // 心跳消息，不需要特殊处理
            return;
        }
        
        // 处理业务消息
        Log.d(TAG, "处理业务消息: " + message);
        
        // 这里可以根据消息格式进行解析和处理
        // 例如：JSON 解析、消息路由等
    }
    
    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return webSocketManager != null && webSocketManager.isConnected();
    }
    
    /**
     * 获取连接状态描述
     */
    public String getConnectionState() {
        if (webSocketManager == null) {
            return "未初始化";
        }
        
        WebSocketManager.ConnectionState state = webSocketManager.getConnectionState();
        switch (state) {
            case DISCONNECTED:
                return "未连接";
            case CONNECTING:
                return "连接中";
            case CONNECTED:
                return "已连接";
            case RECONNECTING:
                return "重连中";
            default:
                return "未知状态";
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (webSocketManager != null) {
            webSocketManager.release();
            webSocketManager = null;
        }
    }
    
    /**
     * 使用示例
     */
    public static void main(String[] args) {
        WebSocketExample example = new WebSocketExample();
        
        // 连接
        example.connect();
        
        // 等待一段时间后发送消息
        try {
            Thread.sleep(2000);
            example.sendMessage("测试消息");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 等待一段时间后断开连接
        try {
            Thread.sleep(5000);
            example.disconnect();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 释放资源
        example.release();
    }
}

