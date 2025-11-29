package com.wait.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 响应工具类 - 统一构建API响应格式
 */
public class ResponseUtil {

    /**
     * 构建成功响应
     */
    public static ResponseEntity<Map<String, Object>> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        if (data != null) {
            response.put("data", data);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 构建成功响应（带消息）
     */
    public static ResponseEntity<Map<String, Object>> success(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 构建成功响应（仅消息）
     */
    public static ResponseEntity<Map<String, Object>> success(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return ResponseEntity.ok(response);
    }

    /**
     * 构建带额外字段的成功响应
     */
    public static ResponseEntity<Map<String, Object>> success(Map<String, Object> extraFields, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        if (data != null) {
            response.put("data", data);
        }
        if (extraFields != null) {
            response.putAll(extraFields);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 构建错误响应
     */
    public static ResponseEntity<Map<String, Object>> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 构建错误响应（带状态码）
     */
    public static ResponseEntity<Map<String, Object>> error(int statusCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("code", statusCode);
        return ResponseEntity.status(HttpStatus.valueOf(statusCode)).body(response);
    }

    /**
     * 构建错误响应（带状态码和数据）
     */
    public static ResponseEntity<Map<String, Object>> error(int statusCode, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("code", statusCode);
        if (data != null) {
            response.put("data", data);
        }
        return ResponseEntity.status(HttpStatus.valueOf(statusCode)).body(response);
    }

    /**
     * 构建错误响应（使用 HttpStatus 枚举）
     */
    public static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("code", status.value());
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 构建错误响应（使用 HttpStatus 枚举，带数据）
     */
    public static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("code", status.value());
        if (data != null) {
            response.put("data", data);
        }
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 构建内部服务器错误响应
     */
    public static ResponseEntity<Map<String, Object>> internalError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * 构建未找到错误响应
     */
    public static ResponseEntity<Map<String, Object>> notFound(String message) {
        return error(HttpStatus.NOT_FOUND, message);
    }

    /**
     * 构建未授权错误响应
     */
    public static ResponseEntity<Map<String, Object>> unauthorized(String message) {
        return error(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * 构建禁止访问错误响应
     */
    public static ResponseEntity<Map<String, Object>> forbidden(String message) {
        return error(HttpStatus.FORBIDDEN, message);
    }

    /**
     * 构建参数错误响应
     */
    public static ResponseEntity<Map<String, Object>> badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 构建参数错误响应（带验证错误详情）
     */
    public static ResponseEntity<Map<String, Object>> badRequest(String message, Object validationErrors) {
        return error(HttpStatus.BAD_REQUEST, message, validationErrors);
    }
}

