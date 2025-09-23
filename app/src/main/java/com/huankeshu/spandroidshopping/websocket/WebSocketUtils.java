package com.huankeshu.spandroidshopping.websocket;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * WebSocket 工具类
 * 提供常用的消息格式化和处理功能
 */
public class WebSocketUtils {
    
    private static final String TAG = "WebSocketUtils";
    
    /**
     * 消息类型常量
     */
    public static class MessageType {
        public static final String HEARTBEAT = "heartbeat";
        public static final String TEXT = "text";
        public static final String JSON = "json";
        public static final String BINARY = "binary";
        public static final String ERROR = "error";
        public static final String NOTIFICATION = "notification";
    }
    
    /**
     * 创建心跳消息
     */
    public static String createHeartbeatMessage() {
        return createJsonMessage(MessageType.HEARTBEAT, "ping", null);
    }
    
    /**
     * 创建文本消息
     */
    public static String createTextMessage(String content) {
        return createJsonMessage(MessageType.TEXT, content, null);
    }
    
    /**
     * 创建JSON消息
     */
    public static String createJsonMessage(String type, String content, JSONObject data) {
        try {
            JSONObject message = new JSONObject();
            message.put("type", type);
            message.put("content", content);
            message.put("timestamp", System.currentTimeMillis());
            
            if (data != null) {
                message.put("data", data);
            }
            
            return message.toString();
        } catch (JSONException e) {
            Log.e(TAG, "创建JSON消息失败", e);
            return createTextMessage(content);
        }
    }
    
    /**
     * 创建错误消息
     */
    public static String createErrorMessage(String error) {
        return createJsonMessage(MessageType.ERROR, error, null);
    }
    
    /**
     * 创建通知消息
     */
    public static String createNotificationMessage(String title, String content) {
        try {
            JSONObject data = new JSONObject();
            data.put("title", title);
            data.put("content", content);
            return createJsonMessage(MessageType.NOTIFICATION, content, data);
        } catch (JSONException e) {
            Log.e(TAG, "创建通知消息失败", e);
            return createTextMessage(content);
        }
    }
    
    /**
     * 解析消息
     */
    public static WebSocketMessage parseMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type", MessageType.TEXT);
            String content = json.optString("content", message);
            long timestamp = json.optLong("timestamp", System.currentTimeMillis());
            JSONObject data = json.optJSONObject("data");
            
            return new WebSocketMessage(type, content, timestamp, data);
        } catch (JSONException e) {
            Log.d(TAG, "消息不是JSON格式，作为普通文本处理");
            return new WebSocketMessage(MessageType.TEXT, message, System.currentTimeMillis(), null);
        }
    }
    
    /**
     * 检查是否为心跳消息
     */
    public static boolean isHeartbeatMessage(String message) {
        WebSocketMessage msg = parseMessage(message);
        return MessageType.HEARTBEAT.equals(msg.getType()) || 
               "ping".equals(message) || 
               "pong".equals(message);
    }
    
    /**
     * 检查是否为错误消息
     */
    public static boolean isErrorMessage(String message) {
        WebSocketMessage msg = parseMessage(message);
        return MessageType.ERROR.equals(msg.getType());
    }
    
    /**
     * 格式化连接URL
     */
    public static String formatWebSocketUrl(String host, int port, String path) {
        StringBuilder url = new StringBuilder();
        url.append("ws://").append(host);
        
        if (port > 0 && port != 80) {
            url.append(":").append(port);
        }
        
        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/")) {
                url.append("/");
            }
            url.append(path);
        }
        
        return url.toString();
    }
    
    /**
     * 格式化安全连接URL
     */
    public static String formatSecureWebSocketUrl(String host, int port, String path) {
        StringBuilder url = new StringBuilder();
        url.append("wss://").append(host);
        
        if (port > 0 && port != 443) {
            url.append(":").append(port);
        }
        
        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/")) {
                url.append("/");
            }
            url.append(path);
        }
        
        return url.toString();
    }
    
    /**
     * 验证WebSocket URL格式
     */
    public static boolean isValidWebSocketUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        return url.startsWith("ws://") || url.startsWith("wss://");
    }
    
    /**
     * WebSocket 消息封装类
     */
    public static class WebSocketMessage {
        private String type;
        private String content;
        private long timestamp;
        private JSONObject data;
        
        public WebSocketMessage(String type, String content, long timestamp, JSONObject data) {
            this.type = type;
            this.content = content;
            this.timestamp = timestamp;
            this.data = data;
        }
        
        public String getType() {
            return type;
        }
        
        public String getContent() {
            return content;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public JSONObject getData() {
            return data;
        }
        
        public String getDataString(String key) {
            if (data != null) {
                return data.optString(key, "");
            }
            return "";
        }
        
        public int getDataInt(String key, int defaultValue) {
            if (data != null) {
                return data.optInt(key, defaultValue);
            }
            return defaultValue;
        }
        
        public boolean getDataBoolean(String key, boolean defaultValue) {
            if (data != null) {
                return data.optBoolean(key, defaultValue);
            }
            return defaultValue;
        }
        
        @Override
        public String toString() {
            return "WebSocketMessage{" +
                    "type='" + type + '\'' +
                    ", content='" + content + '\'' +
                    ", timestamp=" + timestamp +
                    ", data=" + (data != null ? data.toString() : "null") +
                    '}';
        }
    }
}

