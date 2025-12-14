package com.wait.task.persist;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.wait.entity.domain.UVStatistics;
import com.wait.mapper.UVStatisticsMapper;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * UV统计数据持久化服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UVStatisticsPersistenceService implements DataPersistenceService<UVStatistics> {

    private final BoundUtil boundUtil;
    private final UVStatisticsMapper uvStatisticsMapper;

    private static final String UV_DAILY_PREFIX = "uv:daily:";
    private static final int KEEP_DAYS = 7;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public String getKeyPattern() {
        return UV_DAILY_PREFIX + "*";
    }

    @Override
    public int getKeepDays() {
        return KEEP_DAYS;
    }

    @Override
    public Long parseExpireTimeFromKey(String key) {
        try {
            // 解析 key: uv:daily:post:123:20240101
            // 格式：uv:daily:{resourceType}:{resourceId}:{date}
            String[] parts = key.substring(UV_DAILY_PREFIX.length()).split(":");
            if (parts.length < 3) {
                return null;
            }

            // 提取日期（最后一部分）
            String date = parts[parts.length - 1];
            LocalDate keyDate = LocalDate.parse(date, DATE_FORMATTER);
            return keyDate.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();
        } catch (Exception e) {
            log.warn("Failed to parse expire time from key: {}", key, e);
            return null;
        }
    }

    @Override
    public List<UVStatistics> collectDataFromRedis(String key, long expireTime) {
        List<UVStatistics> result = new ArrayList<>();
        
        try {
            // 解析 key: uv:daily:post:123:20240101
            String[] parts = key.substring(UV_DAILY_PREFIX.length()).split(":");
            if (parts.length < 3) {
                log.warn("Invalid UV key format: {}", key);
                return result;
            }

            // 提取日期（最后一部分）
            String date = parts[parts.length - 1];
            LocalDate keyDate = LocalDate.parse(date, DATE_FORMATTER);
            long keyTimestamp = keyDate.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();
            
            // 判断是否过期
            if (keyTimestamp >= expireTime) {
                return result; // 未过期，返回空列表
            }

            // 提取资源类型和ID
            String resourceType = parts[0];
            Long resourceId = Long.parseLong(parts[1]);

            // 获取该 key 的所有访客
            Set<String> visitors = boundUtil.sMembers(key, String.class);
            if (visitors != null && !visitors.isEmpty()) {
                for (String visitorId : visitors) {
                    UVStatistics stat = UVStatistics.builder()
                            .resourceType(resourceType)
                            .resourceId(resourceId)
                            .date(date)
                            .visitorId(visitorId)
                            .build();
                    result.add(stat);
                }
            }
        } catch (Exception e) {
            log.error("Failed to collect data from Redis for key: {}", key, e);
        }

        return result;
    }

    @Override
    public int batchInsertToDatabase(List<UVStatistics> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        return uvStatisticsMapper.batchInsert(dataList);
    }

    @Override
    public long deleteFromRedis(String key, long expireTime) {
        try {
            // 检查 key 的日期是否过期
            Long keyTimestamp = parseExpireTimeFromKey(key);
            if (keyTimestamp != null && keyTimestamp < expireTime) {
                Boolean deleted = boundUtil.del(key);
                return Boolean.TRUE.equals(deleted) ? 1 : 0;
            }
        } catch (Exception e) {
            log.error("Failed to delete from Redis for key: {}", key, e);
        }
        return 0;
    }

    @Override
    public String getTaskName() {
        return "UV Statistics Persistence";
    }
}

