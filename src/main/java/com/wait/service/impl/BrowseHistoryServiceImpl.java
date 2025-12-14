package com.wait.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.wait.config.script.BrowseHistoryScripts;
import com.wait.entity.domain.BrowseHistory;
import com.wait.mapper.BrowseHistoryMapper;
import com.wait.service.BrowseHistoryService;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 浏览记录服务实现
 * 使用 Redis Sorted Set 记录用户浏览历史，支持去重和时间范围查询
 * 
 * Redis 命令使用：
 * - ZADD: 添加浏览记录（自动去重，更新时间为最新）
 * - ZREVRANGE: 获取浏览历史（按时间倒序）
 * - ZRANGEBYSCORE: 按时间范围查询
 * - ZSCORE: 获取浏览时间
 * - ZREM: 删除浏览记录
 * - ZCARD: 获取浏览记录数量
 * - ZREMRANGEBYSCORE: 按时间范围删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrowseHistoryServiceImpl implements BrowseHistoryService {

    private final BoundUtil boundUtil;
    private final BrowseHistoryMapper browseHistoryMapper;
    private final BrowseHistoryScripts browseHistoryScripts;

    private static final String BROWSE_PREFIX = "browse:history:user:";
    private static final int KEEP_DAYS = 3; // Redis中保留最近3天的数据
    private static final int DEFAULT_MAX_RECORDS = 1000; // 默认最多保留1000条记录

    @Override
    public void recordBrowse(Long userId, Long postId) {
        if (userId == null || postId == null) {
            throw new IllegalArgumentException("User ID and Post ID cannot be null");
        }

        String key = BROWSE_PREFIX + userId;
        long currentTime = System.currentTimeMillis();

        // 使用 Lua 脚本原子性地执行：ZADD + ZCARD + ZREMRANGEBYRANK
        try {
            Long removedCount = browseHistoryScripts.executeScript(
                    BrowseHistoryScripts.RECORD_BROWSE,
                    java.util.Arrays.asList(key),
                    String.valueOf(postId),
                    String.valueOf(currentTime),
                    String.valueOf(DEFAULT_MAX_RECORDS));
            if (removedCount != null && removedCount > 0) {
                log.debug("Browse recorded and {} old records removed: user={}, post={}, time={}",
                        removedCount, userId, postId, currentTime);
            } else {
                log.debug("Browse recorded: user={}, post={}, time={}", userId, postId, currentTime);
            }
        } catch (Exception e) {
            log.error("Failed to record browse using Lua script, fallback to normal operations: user={}, post={}",
                    userId, postId, e);
            // 降级处理：使用普通命令
            boundUtil.zAdd(key, postId, currentTime);
            Long size = boundUtil.zCard(key);
            if (size != null && size > DEFAULT_MAX_RECORDS) {
                boundUtil.zRemRangeByRank(key, 0, size - DEFAULT_MAX_RECORDS - 1);
            }
        }
    }

    @Override
    public List<Long> getBrowseHistory(Long userId, int limit) {
        if (userId == null || limit <= 0) {
            return new ArrayList<>();
        }

        String key = BROWSE_PREFIX + userId;

        // 从Redis获取最近N条记录（按时间倒序）
        List<Long> redisPostIds = boundUtil.zReverseRange(key, 0, limit - 1, Long.class);

        if (redisPostIds == null || redisPostIds.isEmpty()) {
            // Redis中没有数据，从数据库查询
            return getBrowseHistoryFromDatabase(userId, limit);
        }

        // 检查是否存在截断情况：如果Redis返回的数据不足limit条，需要从数据库补充
        if (redisPostIds.size() < limit) {
            int remainingCount = limit - redisPostIds.size();
            // 从数据库查询记录，排除Redis中已有的postId
            List<Long> dbPostIds = getBrowseHistoryFromDatabaseExcluding(userId, redisPostIds, remainingCount);

            // 合并结果（Redis中的数据已经在前面，按时间倒序）
            List<Long> result = new ArrayList<>(redisPostIds);
            result.addAll(dbPostIds);

            // 如果合并后超过limit，只返回前limit条
            return result.size() > limit ? result.subList(0, limit) : result;
        }

        return redisPostIds;
    }

    @Override
    public List<Long> getBrowseHistory(Long userId, int page, int pageSize) {
        if (userId == null || page < 1 || pageSize <= 0) {
            return new ArrayList<>();
        }

        String key = BROWSE_PREFIX + userId;
        long start = (page - 1) * pageSize;
        long end = start + pageSize - 1;

        // 从Redis获取分页数据（按时间倒序）
        List<Long> redisPostIds = boundUtil.zReverseRange(key, start, end, Long.class);

        if (redisPostIds == null || redisPostIds.isEmpty()) {
            // Redis中没有数据，从数据库查询
            return getBrowseHistoryFromDatabase(userId, page, pageSize);
        }

        // 检查是否存在截断情况：如果Redis返回的数据不足pageSize条，需要从数据库补充
        if (redisPostIds.size() < pageSize) {
            // 计算需要从数据库补充的数量
            int remainingCount = pageSize - redisPostIds.size();

            // 从数据库查询记录，排除Redis中已有的postId
            // 注意：由于Redis和数据库可能有重叠（定时任务未执行），需要排除Redis中已有的记录
            List<Long> dbPostIds = getBrowseHistoryFromDatabaseExcluding(userId, redisPostIds, remainingCount);

            // 合并结果（Redis中的数据已经在前面，按时间倒序）
            List<Long> result = new ArrayList<>(redisPostIds);
            result.addAll(dbPostIds);

            // 如果合并后超过pageSize，只返回前pageSize条
            return result.size() > pageSize ? result.subList(0, pageSize) : result;
        }

        return redisPostIds;
    }

    @Override
    public List<Long> getBrowseHistoryByTimeRange(Long userId, long startTime, long endTime) {
        if (userId == null) {
            return new ArrayList<>();
        }

        String key = BROWSE_PREFIX + userId;

        // 从Redis查询时间范围内的记录（按时间倒序）
        // 注意：zRevRangeByScore 参数顺序是 (maxScore, minScore)，即 (endTime, startTime)
        List<Long> redisPostIds = boundUtil.zRevRangeByScore(key, endTime, startTime, Long.class);

        // 从数据库查询3天前的记录
        long threeDaysAgo = System.currentTimeMillis() - (KEEP_DAYS * 24L * 60 * 60 * 1000);
        List<Long> dbPostIds = new ArrayList<>();

        if (startTime < threeDaysAgo) {
            // 需要查询数据库
            List<BrowseHistory> dbRecords = browseHistoryMapper.selectByUserIdAndTimeRange(
                    userId, startTime, Math.min(endTime, threeDaysAgo));
            dbPostIds = dbRecords.stream()
                    .map(BrowseHistory::getPostId)
                    .collect(Collectors.toList());
        }

        // 合并Redis和数据库的结果（去重，按时间倒序）
        List<Long> result = new ArrayList<>();
        if (redisPostIds != null && !redisPostIds.isEmpty()) {
            result.addAll(redisPostIds);
        }
        result.addAll(dbPostIds);

        // 去重（保留最新的）
        return result.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public boolean hasBrowsed(Long userId, Long postId) {
        if (userId == null || postId == null) {
            return false;
        }

        String key = BROWSE_PREFIX + userId;

        // 先检查Redis
        Double score = boundUtil.zScore(key, postId);
        if (score != null) {
            return true;
        }

        // 检查数据库
        return browseHistoryMapper.existsByUserIdAndPostId(userId, postId);
    }

    @Override
    public Long getBrowseTime(Long userId, Long postId) {
        if (userId == null || postId == null) {
            return null;
        }

        String key = BROWSE_PREFIX + userId;

        // 先检查Redis
        Double score = boundUtil.zScore(key, postId);
        if (score != null) {
            return score.longValue();
        }

        // 从数据库查询
        BrowseHistory history = browseHistoryMapper.selectByUserIdAndPostId(userId, postId);
        return history != null ? history.getBrowseTime() : null;
    }

    @Override
    public void clearBrowseHistory(Long userId) {
        if (userId == null) {
            return;
        }

        String key = BROWSE_PREFIX + userId;

        // 删除Redis中的记录
        boundUtil.del(key);

        // 删除数据库中的记录
        browseHistoryMapper.deleteByUserId(userId);

        log.info("Browse history cleared for user: {}", userId);
    }

    @Override
    public void clearOldBrowseHistory(Long userId, long expireTime) {
        if (userId == null) {
            return;
        }

        String key = BROWSE_PREFIX + userId;

        // 删除Redis中过期的记录
        Long deletedFromRedis = boundUtil.zRemRangeByScore(key, 0, expireTime);

        // 删除数据库中过期的记录
        int deletedFromDb = browseHistoryMapper.deleteByUserIdAndExpireTime(userId, expireTime);

        log.info("Cleared old browse history for user {}: {} from Redis, {} from database",
                userId, deletedFromRedis != null ? deletedFromRedis : 0, deletedFromDb);
    }

    @Override
    public Long getBrowseHistoryCount(Long userId) {
        if (userId == null) {
            return 0L;
        }

        String key = BROWSE_PREFIX + userId;

        // Redis中的记录数
        Long redisCount = boundUtil.zCard(key);

        // 数据库中的记录数
        Long dbCount = browseHistoryMapper.countByUserId(userId);

        return (redisCount != null ? redisCount : 0L) + (dbCount != null ? dbCount : 0L);
    }

    /**
     * 从数据库获取浏览历史
     */
    private List<Long> getBrowseHistoryFromDatabase(Long userId, int limit) {
        List<BrowseHistory> records = browseHistoryMapper.selectByUserId(userId, 0, limit);
        return records.stream()
                .map(BrowseHistory::getPostId)
                .collect(Collectors.toList());
    }

    /**
     * 从数据库获取浏览历史（分页）
     */
    private List<Long> getBrowseHistoryFromDatabase(Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<BrowseHistory> records = browseHistoryMapper.selectByUserId(userId, offset, pageSize);
        return records.stream()
                .map(BrowseHistory::getPostId)
                .collect(Collectors.toList());
    }

    /**
     * 从数据库获取浏览历史，排除指定的postId列表
     * 用于补充Redis中不足的数据，避免重复
     * 
     * @param userId         用户ID
     * @param excludePostIds 需要排除的postId列表（Redis中已有的）
     * @param limit          需要获取的数量
     * @return 浏览记录postId列表
     */
    private List<Long> getBrowseHistoryFromDatabaseExcluding(Long userId, List<Long> excludePostIds, int limit) {
        if (excludePostIds == null || excludePostIds.isEmpty()) {
            // 如果没有需要排除的，直接查询
            return getBrowseHistoryFromDatabase(userId, 0, limit);
        }

        // 从数据库查询更多记录，然后排除Redis中已有的
        // 注意：这里查询 limit * 2 条记录，因为可能有重叠需要排除
        List<BrowseHistory> records = browseHistoryMapper.selectByUserId(userId, 0, limit * 2);
        List<Long> result = new ArrayList<>();
        Set<Long> excludeSet = new HashSet<>(excludePostIds);

        for (BrowseHistory record : records) {
            if (result.size() >= limit) {
                break;
            }
            Long postId = record.getPostId();
            if (postId != null && !excludeSet.contains(postId)) {
                result.add(postId);
            }
        }

        return result;
    }

}
