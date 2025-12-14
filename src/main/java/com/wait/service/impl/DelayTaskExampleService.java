package com.wait.service.impl;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.wait.service.DelayQueueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 延迟队列使用示例服务
 * 演示如何在项目中使用 DelayQueueService
 * 
 * 使用场景：
 * 1. 订单超时自动取消
 * 2. 优惠券过期提醒
 * 3. 定时推送消息
 * 4. 数据同步任务
 * 5. 关系数据校验任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelayTaskExampleService implements CommandLineRunner {

    private final DelayQueueService delayQueueService;

    // 队列名称常量
    private static final String ORDER_CANCEL_QUEUE = "order:cancel";
    private static final String COUPON_EXPIRE_QUEUE = "coupon:expire";
    private static final String NOTIFICATION_QUEUE = "notification:push";
    private static final String DATA_SYNC_QUEUE = "data:sync";
    private static final String RELATION_VALIDATION_QUEUE = "relation:validation";

    @Override
    public void run(String... args) {
        // 注册处理器并启动消费
        registerHandlers();
        startConsuming();
        
        log.info("Delay queue example service initialized");
    }

    /**
     * 注册所有队列的处理器
     */
    private void registerHandlers() {
        // 1. 订单取消处理器
        delayQueueService.registerHandler(ORDER_CANCEL_QUEUE, this::handleOrderCancel);
        
        // 2. 优惠券过期处理器
        delayQueueService.registerHandler(COUPON_EXPIRE_QUEUE, this::handleCouponExpire);
        
        // 3. 通知推送处理器
        delayQueueService.registerHandler(NOTIFICATION_QUEUE, this::handleNotificationPush);
        
        // 4. 数据同步处理器
        delayQueueService.registerHandler(DATA_SYNC_QUEUE, this::handleDataSync);
        
        // 5. 关系数据校验处理器
        delayQueueService.registerHandler(RELATION_VALIDATION_QUEUE, this::handleRelationValidation);
    }

    /**
     * 启动所有队列的消费
     */
    private void startConsuming() {
        delayQueueService.startConsuming(ORDER_CANCEL_QUEUE);
        delayQueueService.startConsuming(COUPON_EXPIRE_QUEUE);
        delayQueueService.startConsuming(NOTIFICATION_QUEUE);
        delayQueueService.startConsuming(DATA_SYNC_QUEUE);
        delayQueueService.startConsuming(RELATION_VALIDATION_QUEUE);
    }

    /**
     * 示例：添加订单取消任务（15分钟后自动取消）
     */
    public void scheduleOrderCancel(String orderId, long delayMinutes) {
        long delaySeconds = delayMinutes * 60;
        delayQueueService.addTaskWithDelay(ORDER_CANCEL_QUEUE, orderId, delaySeconds);
        log.info("Order cancel task scheduled: orderId={}, delay={} minutes", orderId, delayMinutes);
    }

    /**
     * 示例：添加优惠券过期提醒任务（过期前1小时提醒）
     */
    public void scheduleCouponExpireReminder(String couponId, long expireTime) {
        // 提前1小时提醒
        long reminderTime = expireTime - 3600 * 1000;
        delayQueueService.addTask(COUPON_EXPIRE_QUEUE, couponId, reminderTime);
        log.info("Coupon expire reminder scheduled: couponId={}, reminderTime={}", couponId, reminderTime);
    }

    /**
     * 示例：添加定时推送通知任务
     */
    public void scheduleNotificationPush(String notificationId, long delaySeconds) {
        delayQueueService.addTaskWithDelay(NOTIFICATION_QUEUE, notificationId, delaySeconds);
        log.info("Notification push task scheduled: notificationId={}, delay={} seconds", notificationId, delaySeconds);
    }

    /**
     * 示例：添加数据同步任务
     */
    public void scheduleDataSync(String syncTaskId, long executeTime) {
        delayQueueService.addTask(DATA_SYNC_QUEUE, syncTaskId, executeTime);
        log.info("Data sync task scheduled: syncTaskId={}, executeTime={}", syncTaskId, executeTime);
    }

    /**
     * 示例：添加关系数据校验任务
     */
    public void scheduleRelationValidation(String validationTaskId, long delayMinutes) {
        long delaySeconds = delayMinutes * 60;
        delayQueueService.addTaskWithDelay(RELATION_VALIDATION_QUEUE, validationTaskId, delaySeconds);
        log.info("Relation validation task scheduled: taskId={}, delay={} minutes", validationTaskId, delayMinutes);
    }

    // ==================== 处理器实现 ====================

    /**
     * 订单取消处理器
     */
    private boolean handleOrderCancel(String orderId) {
        try {
            log.info("Processing order cancel: orderId={}", orderId);
            
            //  实现订单取消逻辑
            // 1. 检查订单状态
            // 2. 如果未支付，则取消订单
            // 3. 释放库存
            // 4. 发送取消通知
            
            log.info("Order canceled successfully: orderId={}", orderId);
            return true;
        } catch (Exception e) {
            log.error("Failed to cancel order: orderId={}", orderId, e);
            return false; // 返回false会触发重试
        }
    }

    /**
     * 优惠券过期处理器
     */
    private boolean handleCouponExpire(String couponId) {
        try {
            log.info("Processing coupon expire reminder: couponId={}", couponId);
            
            //  实现优惠券过期提醒逻辑
            // 1. 查询优惠券信息
            // 2. 发送过期提醒通知给用户
            // 3. 更新优惠券状态
            
            log.info("Coupon expire reminder sent: couponId={}", couponId);
            return true;
        } catch (Exception e) {
            log.error("Failed to handle coupon expire: couponId={}", couponId, e);
            return false;
        }
    }

    /**
     * 通知推送处理器
     */
    private boolean handleNotificationPush(String notificationId) {
        try {
            log.info("Processing notification push: notificationId={}", notificationId);
            
            //  实现通知推送逻辑
            // 1. 查询通知内容
            // 2. 推送给目标用户
            // 3. 更新通知状态
            
            log.info("Notification pushed successfully: notificationId={}", notificationId);
            return true;
        } catch (Exception e) {
            log.error("Failed to push notification: notificationId={}", notificationId, e);
            return false;
        }
    }

    /**
     * 数据同步处理器
     */
    private boolean handleDataSync(String syncTaskId) {
        try {
            log.info("Processing data sync: syncTaskId={}", syncTaskId);
            
            //  实现数据同步逻辑
            // 1. 解析同步任务参数
            // 2. 执行数据同步
            // 3. 记录同步结果
            
            log.info("Data sync completed: syncTaskId={}", syncTaskId);
            return true;
        } catch (Exception e) {
            log.error("Failed to sync data: syncTaskId={}", syncTaskId, e);
            return false;
        }
    }

    /**
     * 关系数据校验处理器
     */
    private boolean handleRelationValidation(String validationTaskId) {
        try {
            log.info("Processing relation validation: taskId={}", validationTaskId);
            
            //  实现关系数据校验逻辑
            // 1. 解析校验任务参数（如postId、userId等）
            // 2. 调用 RelationDataValidationService 进行校验
            // 3. 记录校验结果
            
            log.info("Relation validation completed: taskId={}", validationTaskId);
            return true;
        } catch (Exception e) {
            log.error("Failed to validate relation data: taskId={}", validationTaskId, e);
            return false;
        }
    }
}

