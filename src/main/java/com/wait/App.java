package com.wait;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.wait.config.TimeWindowStatisticsProperties;

@SpringBootApplication
@EnableScheduling  // 启用定时任务支持
@EnableConfigurationProperties(TimeWindowStatisticsProperties.class)  // 启用配置属性类
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}