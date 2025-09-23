package com.huankeshu.spandroidshopping.websocket;

/**
 * WebSocket 状态监听器接口
 */
public interface WebSocketListener {
    
    /**
     * 连接成功回调
     */
    void onConnected();
    
    /**
     * 连接失败回调
     * @param error 错误信息
     */
    void onConnectionFailed(String error);
    
    /**
     * 连接断开回调
     * @param code 断开代码
     * @param reason 断开原因
     */
    void onDisconnected(int code, String reason);
    
    /**
     * 接收到消息回调
     * @param message 消息内容
     */
    void onMessageReceived(String message);
    
    /**
     * 发送消息失败回调
     * @param error 错误信息
     */
    void onSendFailed(String error);
    
    /**
     * 重连开始回调
     * @param attempt 重连次数
     */
    void onReconnecting(int attempt);
    
    /**
     * 重连成功回调
     */
    void onReconnected();
    
    /**
     * 重连失败回调
     * @param error 错误信息
     */
    void onReconnectFailed(String error);
}
