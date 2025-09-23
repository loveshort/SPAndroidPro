package com.huankeshu.spandroidshopping.websocket;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * WebSocket 连接管理器
 * 提供连接、断开、重连、心跳检测等功能
 */
public class WebSocketManager {
    
    private static final String TAG = "WebSocketManager";
    
    // 连接状态
    public enum ConnectionState {
        DISCONNECTED,    // 未连接
        CONNECTING,      // 连接中
        CONNECTED,       // 已连接
        RECONNECTING     // 重连中
    }
    
    // 默认配置
    private static final int DEFAULT_RECONNECT_INTERVAL = 3000; // 3秒
    private static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 5; // 最大重连次数
    private static final int DEFAULT_HEARTBEAT_INTERVAL = 30000; // 30秒心跳
    private static final int DEFAULT_CONNECT_TIMEOUT = 10000; // 10秒连接超时
    private static final int DEFAULT_READ_TIMEOUT = 10000; // 10秒读取超时
    private static final int DEFAULT_WRITE_TIMEOUT = 10000; // 10秒写入超时
    
    // 核心组件
    private OkHttpClient httpClient;
    private WebSocket webSocket;
    private WebSocketListener listener;
    private Handler mainHandler;
    
    // 连接配置
    private String serverUrl;
    private int reconnectInterval = DEFAULT_RECONNECT_INTERVAL;
    private int maxReconnectAttempts = DEFAULT_MAX_RECONNECT_ATTEMPTS;
    private int heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
    private String heartbeatMessage = "ping";
    
    // 状态管理
    private volatile ConnectionState currentState = ConnectionState.DISCONNECTED;
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicBoolean shouldReconnect = new AtomicBoolean(true);
    
    // 心跳相关
    private Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    
    // 重连相关
    private Handler reconnectHandler;
    private Runnable reconnectRunnable;
    
    public WebSocketManager() {
        init();
    }
    
    public WebSocketManager(String serverUrl) {
        this.serverUrl = serverUrl;
        init();
    }
    
    /**
     * 初始化
     */
    private void init() {
        mainHandler = new Handler(Looper.getMainLooper());
        heartbeatHandler = new Handler(Looper.getMainLooper());
        reconnectHandler = new Handler(Looper.getMainLooper());
        
        // 创建 OkHttpClient
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        
        // 创建 WebSocket 监听器
        listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket 连接成功");
                currentState = ConnectionState.CONNECTED;
                reconnectAttempts.set(0);
                isReconnecting.set(false);
                
                // 启动心跳
                startHeartbeat();
                
                // 回调到主线程
                if (webSocketListener != null) {
                    mainHandler.post(() -> {
                        if (isReconnecting.get()) {
                            webSocketListener.onReconnected();
                        } else {
                            webSocketListener.onConnected();
                        }
                    });
                }
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "收到消息: " + text);
                
                // 处理心跳响应
                if (heartbeatMessage.equals(text) || "pong".equals(text)) {
                    Log.d(TAG, "收到心跳响应");
                    return;
                }
                
                // 回调到主线程
                if (webSocketListener != null) {
                    mainHandler.post(() -> webSocketListener.onMessageReceived(text));
                }
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket 正在关闭: " + code + " - " + reason);
                currentState = ConnectionState.DISCONNECTED;
                stopHeartbeat();
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket 已关闭: " + code + " - " + reason);
                currentState = ConnectionState.DISCONNECTED;
                stopHeartbeat();
                
                // 回调到主线程
                if (webSocketListener != null) {
                    mainHandler.post(() -> webSocketListener.onDisconnected(code, reason));
                }
                
                // 尝试重连
                if (shouldReconnect.get() && !isReconnecting.get()) {
                    scheduleReconnect();
                }
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket 连接失败", t);
                currentState = ConnectionState.DISCONNECTED;
                stopHeartbeat();
                
                String error = t != null ? t.getMessage() : "未知错误";
                
                // 回调到主线程
                if (webSocketListener != null) {
                    mainHandler.post(() -> {
                        if (isReconnecting.get()) {
                            webSocketListener.onReconnectFailed(error);
                        } else {
                            webSocketListener.onConnectionFailed(error);
                        }
                    });
                }
                
                // 尝试重连
                if (shouldReconnect.get() && !isReconnecting.get()) {
                    scheduleReconnect();
                }
            }
        };
    }
    
    /**
     * 设置服务器URL
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    /**
     * 设置重连间隔
     */
    public void setReconnectInterval(int interval) {
        this.reconnectInterval = interval;
    }
    
    /**
     * 设置最大重连次数
     */
    public void setMaxReconnectAttempts(int maxAttempts) {
        this.maxReconnectAttempts = maxAttempts;
    }
    
    /**
     * 设置心跳间隔
     */
    public void setHeartbeatInterval(int interval) {
        this.heartbeatInterval = interval;
    }
    
    /**
     * 设置心跳消息
     */
    public void setHeartbeatMessage(String message) {
        this.heartbeatMessage = message;
    }
    
    /**
     * 连接WebSocket
     */
    public void connect() {
        if (serverUrl == null || serverUrl.isEmpty()) {
            Log.e(TAG, "服务器URL不能为空");
            if (webSocketListener != null) {
                mainHandler.post(() -> webSocketListener.onConnectionFailed("服务器URL不能为空"));
            }
            return;
        }
        
        if (currentState == ConnectionState.CONNECTED || currentState == ConnectionState.CONNECTING) {
            Log.w(TAG, "WebSocket 已经连接或正在连接中");
            return;
        }
        
        Log.d(TAG, "开始连接 WebSocket: " + serverUrl);
        currentState = ConnectionState.CONNECTING;
        shouldReconnect.set(true);
        
        Request request = new Request.Builder()
                .url(serverUrl)
                .build();
        
        webSocket = httpClient.newWebSocket(request, listener);
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        Log.d(TAG, "主动断开 WebSocket 连接");
        shouldReconnect.set(false);
        stopHeartbeat();
        stopReconnect();
        
        if (webSocket != null) {
            webSocket.close(1000, "主动断开连接");
            webSocket = null;
        }
        
        currentState = ConnectionState.DISCONNECTED;
    }
    
    /**
     * 发送消息
     */
    public boolean sendMessage(String message) {
        if (webSocket == null || currentState != ConnectionState.CONNECTED) {
            Log.e(TAG, "WebSocket 未连接，无法发送消息");
            if (webSocketListener != null) {
                mainHandler.post(() -> webSocketListener.onSendFailed("WebSocket 未连接"));
            }
            return false;
        }
        
        boolean success = webSocket.send(message);
        if (!success) {
            Log.e(TAG, "发送消息失败: " + message);
            if (webSocketListener != null) {
                mainHandler.post(() -> webSocketListener.onSendFailed("发送消息失败"));
            }
        } else {
            Log.d(TAG, "发送消息成功: " + message);
        }
        
        return success;
    }
    
    /**
     * 获取当前连接状态
     */
    public ConnectionState getConnectionState() {
        return currentState;
    }
    
    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return currentState == ConnectionState.CONNECTED;
    }
    
    /**
     * 启动心跳
     */
    private void startHeartbeat() {
        stopHeartbeat();
        
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentState == ConnectionState.CONNECTED && webSocket != null) {
                    Log.d(TAG, "发送心跳: " + heartbeatMessage);
                    boolean success = webSocket.send(heartbeatMessage);
                    if (!success) {
                        Log.e(TAG, "心跳发送失败");
                    }
                    
                    // 安排下次心跳
                    heartbeatHandler.postDelayed(this, heartbeatInterval);
                }
            }
        };
        
        heartbeatHandler.postDelayed(heartbeatRunnable, heartbeatInterval);
    }
    
    /**
     * 停止心跳
     */
    private void stopHeartbeat() {
        if (heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
            heartbeatRunnable = null;
        }
    }
    
    /**
     * 安排重连
     */
    private void scheduleReconnect() {
        if (!shouldReconnect.get() || isReconnecting.get()) {
            return;
        }
        
        int attempts = reconnectAttempts.get();
        if (attempts >= maxReconnectAttempts) {
            Log.e(TAG, "达到最大重连次数，停止重连");
            isReconnecting.set(false);
            if (webSocketListener != null) {
                mainHandler.post(() -> webSocketListener.onReconnectFailed("达到最大重连次数"));
            }
            return;
        }
        
        isReconnecting.set(true);
        currentState = ConnectionState.RECONNECTING;
        int nextAttempt = attempts + 1;
        reconnectAttempts.set(nextAttempt);
        
        Log.d(TAG, "安排重连，第 " + nextAttempt + " 次，延迟 " + reconnectInterval + "ms");
        
        if (webSocketListener != null) {
            mainHandler.post(() -> webSocketListener.onReconnecting(nextAttempt));
        }
        
        reconnectRunnable = () -> {
            if (shouldReconnect.get()) {
                Log.d(TAG, "开始重连，第 " + nextAttempt + " 次");
                connect();
            }
        };
        
        reconnectHandler.postDelayed(reconnectRunnable, reconnectInterval);
    }
    
    /**
     * 停止重连
     */
    private void stopReconnect() {
        if (reconnectRunnable != null) {
            reconnectHandler.removeCallbacks(reconnectRunnable);
            reconnectRunnable = null;
        }
        isReconnecting.set(false);
        reconnectAttempts.set(0);
    }
    
    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放 WebSocket 资源");
        disconnect();
        
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
        
        if (heartbeatHandler != null) {
            heartbeatHandler.removeCallbacksAndMessages(null);
        }
        
        if (reconnectHandler != null) {
            reconnectHandler.removeCallbacksAndMessages(null);
        }
    }
    
    // 外部监听器
    private WebSocketListener webSocketListener;
    
    /**
     * 设置WebSocket监听器
     */
    public void setWebSocketListener(WebSocketListener listener) {
        this.webSocketListener = listener;
    }
    
    /**
     * 移除WebSocket监听器
     */
    public void removeWebSocketListener() {
        this.webSocketListener = null;
    }
}

