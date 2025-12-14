package com.wait.task.persist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.wait.entity.domain.BrowseHistory;
import com.wait.mapper.BrowseHistoryMapper;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 浏览历史数据持久化服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrowseHistoryPersistenceService implements DataPersistenceService<BrowseHistory> {

    private final BoundUtil boundUtil;
    private final BrowseHistoryMapper browseHistoryMapper;

    private static final String BROWSE_PREFIX = "browse:history:user:";
    private static final int KEEP_DAYS = 3;

    @Override
    public String getKeyPattern() {
        return BROWSE_PREFIX + "*";
    }

    @Override
    public int getKeepDays() {
        return KEEP_DAYS;
    }

    @Override
    public Long parseExpireTimeFromKey(String key) {
        // 浏览历史的过期时间不是从 key 中解析，而是从 Sorted Set 的 score 中获取
        // 这里返回 null，表示需要从数据中判断
        return null;
    }

    @Override
    public List<BrowseHistory> collectDataFromRedis(String key, long expireTime) {
        List<BrowseHistory> result = new ArrayList<>();
        
        try {
            // 从key中提取userId: browse:history:user:123 -> 123
            String userIdStr = key.substring(BROWSE_PREFIX.length());
            Long userId = Long.parseLong(userIdStr);

            // 使用 ZRANGEBYSCORE WITHSCORES 一次性获取时间范围内的记录及其分数
            // 避免 N+1 查询问题
            Map<Long, Double> membersWithScores = boundUtil.zRangeByScoreWithScores(
                    key, 0, expireTime, Long.class);
            
            if (membersWithScores != null && !membersWithScores.isEmpty()) {
                for (Map.Entry<Long, Double> entry : membersWithScores.entrySet()) {
                    Long postId = entry.getKey();
                    Double score = entry.getValue();
                    if (postId != null && score != null) {
                        BrowseHistory history = BrowseHistory.builder()
                                .userId(userId)
                                .postId(postId)
                                .browseTime(score.longValue())
                                .build();
                        result.add(history);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to collect data from Redis for key: {}", key, e);
        }

        return result;
    }

    @Override
    public int batchInsertToDatabase(List<BrowseHistory> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        return browseHistoryMapper.batchInsert(dataList);
    }

    @Override
    public long deleteFromRedis(String key, long expireTime) {
        try {
            Long deletedCount = boundUtil.zRemRangeByScore(key, 0, expireTime);
            return deletedCount != null ? deletedCount : 0;
        } catch (Exception e) {
            log.error("Failed to delete from Redis for key: {}", key, e);
            return 0;
        }
    }

    @Override
    public String getTaskName() {
        return "Browse History Persistence";
    }
}

