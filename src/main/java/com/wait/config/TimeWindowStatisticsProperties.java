package com.wait.config;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 时间窗口统计配置属性
 * 从 application.yml 中读取配置
 * 
 * 注意：需要在主类上使用 @EnableConfigurationProperties(TimeWindowStatisticsProperties.class)
 * 启用
 */
@Data
@ConfigurationProperties(prefix = "time-window-statistics")
public class TimeWindowStatisticsProperties {

    /**
     * 需要持久化到数据库的指标名称列表
     * 基础指标名称，不包含动态后缀（如 post:view，而不是 post:view:123）
     */
    private List<String> metrics;

    /**
     * 获取指标名称列表
     * 
     * @return 指标名称列表，如果未配置则返回空列表
     */
    public List<String> getMetrics() {
        return metrics != null ? metrics : Collections.emptyList();
    }
}
