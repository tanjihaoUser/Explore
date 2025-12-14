package com.wait.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wait.entity.domain.PostFavorite;
import com.wait.entity.domain.PostLike;
import com.wait.entity.domain.UserBlock;
import com.wait.entity.domain.UserFollow;
import com.wait.entity.dto.ValidationResult;
import com.wait.mapper.FollowMapper;
import com.wait.mapper.PostFavoriteMapper;
import com.wait.mapper.PostLikeMapper;
import com.wait.mapper.UserBlockMapper;
import com.wait.service.RelationDataValidationService;
import com.wait.service.RelationPersistenceService;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 关系数据校验服务实现
 * 
 * 定时校验Redis和数据库的一致性，确保数据同步
 * 
 * 校验策略：
 * 1. 定时校验：每N分钟执行一次全量或抽样校验
 * 2. 差异修复：发现不一致时，以Redis为准修复数据库（因为Redis是实时数据源）
 * 3. 日志记录：记录所有校验结果，便于监控和排查
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelationDataValidationServiceImpl implements RelationDataValidationService {

    private final BoundUtil boundUtil;
    private final PostLikeMapper postLikeMapper;
    private final PostFavoriteMapper postFavoriteMapper;
    private final FollowMapper followMapper;
    private final UserBlockMapper userBlockMapper;
    private final RelationPersistenceService persistenceService;

    @Qualifier("refreshScheduler")
    private final ThreadPoolTaskScheduler taskScheduler;

    // Redis Key 前缀（与 RelationServiceImpl 保持一致）
    private static final String USER_FOLLOW_PREFIX = "user:follow:";
    private static final String USER_FOLLOWER_PREFIX = "user:follower:";
    private static final String POST_LIKE_PREFIX = "post:like:";
    private static final String USER_FAVORITE_PREFIX = "user:favorite:";
    private static final String USER_BLACKLIST_PREFIX = "user:blacklist:";

    /** 是否启用定时校验 */
    @Value("${relation.validation.enabled:true}")
    private boolean validationEnabled;

    /** 批量校验大小：每次校验N个数据 */
    @Value("${relation.validation.batch-size:100}")
    private int batchValidationSize;

    /** 是否启用定时修复 */
    @Value("${relation.validation.fix.enabled:false}")
    private boolean fixEnabled;

    /** 校验进度跟踪：记录当前校验到哪个批次 */
    private volatile int likeValidationOffset = 0;
    private volatile int favoriteValidationOffset = 0;
    private volatile int followValidationOffset = 0;
    private volatile int blockValidationOffset = 0;

    /** 校验类型轮询：每次定时任务校验不同类型的数据 */
    private volatile int validationTypeIndex = 0;

    @Override
    @Transactional
    public void validateLikeData(Long postId) {
        try {
            if (postId != null) {
                // 校验单个帖子
                validateSinglePostLike(postId);
            } else {
                // 校验所有帖子（抽样或全量，根据实际情况调整）
                log.info("Starting full validation of like data");
                // 这里可以从数据库获取所有有点赞的帖子ID列表，然后逐个校验
                // 为了性能考虑，可以只校验最近活跃的帖子
                log.warn("Full validation of like data not implemented, use specific postId");
            }
        } catch (Exception e) {
            log.error("Failed to validate like data for postId: {}", postId, e);
        }
    }

    /**
     * 校验单个帖子的点赞数据
     */
    private void validateSinglePostLike(Long postId) {
        // 1. 从Redis读取点赞用户列表
        Set<Long> redisLikers = boundUtil.sMembers(POST_LIKE_PREFIX + postId, Long.class);
        long redisCount = redisLikers != null ? redisLikers.size() : 0;

        // 2. 从数据库读取点赞用户列表
        List<Long> dbLikers = postLikeMapper.selectUserIdsByPostId(postId);
        Set<Long> dbLikerSet = new HashSet<>(dbLikers);
        long dbCount = dbLikers.size();

        // 3. 比较差异
        long diffCount = Math.abs(redisCount - dbCount);
        if (diffCount == 0 && redisLikers != null && redisLikers.equals(dbLikerSet)) {
            // 数据一致
            log.debug("Like data consistent for postId: {}, count: {}", postId, redisCount);
            return;
        }

        // 4. 发现不一致，以Redis为准修复数据库
        log.warn("Like data inconsistent for postId: {}, redis: {}, db: {}, diff: {}",
                postId, redisCount, dbCount, diffCount);

        boolean fixed = fixLikeData(postId, redisLikers, dbLikerSet);
        ValidationResult result = new ValidationResult("LIKE", postId, redisCount, dbCount,
                diffCount, fixed);
        log.info("Like data validation result: {}", result);
    }

    /**
     * 修复点赞数据不一致
     */
    private boolean fixLikeData(Long postId, Set<Long> redisLikers, Set<Long> dbLikers) {
        try {
            // 找出需要新增的（在Redis中但不在数据库中）
            List<Long> toInsert = new ArrayList<>();
            if (redisLikers != null) {
                for (Long userId : redisLikers) {
                    if (!dbLikers.contains(userId)) {
                        toInsert.add(userId);
                    }
                }
            }

            // 找出需要删除的（在数据库中但不在Redis中）
            List<Long> toDelete = new ArrayList<>();
            for (Long userId : dbLikers) {
                if (redisLikers == null || !redisLikers.contains(userId)) {
                    toDelete.add(userId);
                }
            }

            // 批量修复
            int fixedCount = 0;
            if (!toInsert.isEmpty()) {
                List<PostLike> insertList = new ArrayList<>();
                for (Long userId : toInsert) {
                    insertList.add(PostLike.builder()
                            .postId(postId)
                            .userId(userId)
                            .build());
                }
                fixedCount += postLikeMapper.batchInsert(insertList);
            }

            if (!toDelete.isEmpty()) {
                List<PostLike> deleteList = new ArrayList<>();
                for (Long userId : toDelete) {
                    deleteList.add(PostLike.builder()
                            .postId(postId)
                            .userId(userId)
                            .build());
                }
                fixedCount += postLikeMapper.batchDelete(deleteList);
            }

            if (fixedCount > 0) {
                log.info("Fixed {} like data inconsistencies for postId: {}", fixedCount, postId);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to fix like data for postId: {}", postId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public void validateFavoriteData(Long userId) {
        try {
            if (userId != null) {
                // 校验单个用户
                validateSingleUserFavorite(userId);
            } else {
                log.warn("Full validation of favorite data not implemented, use specific userId");
            }
        } catch (Exception e) {
            log.error("Failed to validate favorite data for userId: {}", userId, e);
        }
    }

    /**
     * 校验单个用户的收藏数据
     */
    private void validateSingleUserFavorite(Long userId) {
        // 1. 从Redis读取收藏列表
        Set<Long> redisFavorites = boundUtil.sMembers(USER_FAVORITE_PREFIX + userId, Long.class);
        long redisCount = redisFavorites != null ? redisFavorites.size() : 0;

        // 2. 从数据库读取收藏列表
        List<Long> dbFavorites = postFavoriteMapper.selectPostIdsByUserId(userId);
        Set<Long> dbFavoriteSet = new HashSet<>(dbFavorites);
        long dbCount = dbFavorites.size();

        // 3. 比较差异
        long diffCount = Math.abs(redisCount - dbCount);
        if (diffCount == 0 && redisFavorites != null && redisFavorites.equals(dbFavoriteSet)) {
            log.debug("Favorite data consistent for userId: {}, count: {}", userId, redisCount);
            return;
        }

        // 4. 发现不一致，以Redis为准修复数据库
        log.warn("Favorite data inconsistent for userId: {}, redis: {}, db: {}, diff: {}",
                userId, redisCount, dbCount, diffCount);

        boolean fixed = fixFavoriteData(userId, redisFavorites, dbFavoriteSet);
        ValidationResult result = new ValidationResult("FAVORITE", userId, redisCount, dbCount,
                diffCount, fixed);
        log.info("Favorite data validation result: {}", result);
    }

    /**
     * 修复收藏数据不一致
     */
    private boolean fixFavoriteData(Long userId, Set<Long> redisFavorites, Set<Long> dbFavorites) {
        try {
            List<Long> toInsert = new ArrayList<>();
            if (redisFavorites != null) {
                for (Long postId : redisFavorites) {
                    if (!dbFavorites.contains(postId)) {
                        toInsert.add(postId);
                    }
                }
            }

            List<Long> toDelete = new ArrayList<>();
            for (Long postId : dbFavorites) {
                if (redisFavorites == null || !redisFavorites.contains(postId)) {
                    toDelete.add(postId);
                }
            }

            int fixedCount = 0;
            if (!toInsert.isEmpty()) {
                long now = System.currentTimeMillis();
                List<PostFavorite> insertList = new ArrayList<>();
                for (Long postId : toInsert) {
                    insertList.add(PostFavorite.builder()
                            .userId(userId)
                            .postId(postId)
                            .createdAt(now)
                            .build());
                }
                fixedCount += postFavoriteMapper.batchInsert(insertList);
            }

            if (!toDelete.isEmpty()) {
                List<PostFavorite> deleteList = new ArrayList<>();
                for (Long postId : toDelete) {
                    deleteList.add(PostFavorite.builder()
                            .userId(userId)
                            .postId(postId)
                            .build());
                }
                fixedCount += postFavoriteMapper.batchDelete(deleteList);
            }

            if (fixedCount > 0) {
                log.info("Fixed {} favorite data inconsistencies for userId: {}", fixedCount, userId);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to fix favorite data for userId: {}", userId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public void validateFollowData(Long userId) {
        try {
            if (userId != null) {
                validateSingleUserFollow(userId);
            } else {
                log.warn("Full validation of follow data not implemented, use specific userId");
            }
        } catch (Exception e) {
            log.error("Failed to validate follow data for userId: {}", userId, e);
        }
    }

    /**
     * 校验单个用户的关注数据
     */
    private void validateSingleUserFollow(Long userId) {
        // 1. 从Redis读取关注列表
        Set<Long> redisFollowing = boundUtil.sMembers(USER_FOLLOW_PREFIX + userId, Long.class);
        long redisCount = redisFollowing != null ? redisFollowing.size() : 0;

        // 2. 从数据库读取关注列表
        List<Long> dbFollowing = followMapper.selectFollowedIds(userId);
        Set<Long> dbFollowingSet = new HashSet<>(dbFollowing);
        long dbCount = dbFollowing.size();

        // 3. 比较差异
        long diffCount = Math.abs(redisCount - dbCount);
        if (diffCount == 0 && redisFollowing != null && redisFollowing.equals(dbFollowingSet)) {
            log.debug("Follow data consistent for userId: {}, count: {}", userId, redisCount);
            return;
        }

        // 4. 发现不一致，以Redis为准修复数据库
        log.warn("Follow data inconsistent for userId: {}, redis: {}, db: {}, diff: {}",
                userId, redisCount, dbCount, diffCount);

        boolean fixed = fixFollowData(userId, redisFollowing, dbFollowingSet);
        ValidationResult result = new ValidationResult("FOLLOW", userId, redisCount, dbCount,
                diffCount, fixed);
        log.info("Follow data validation result: {}", result);
    }

    /**
     * 修复关注数据不一致
     */
    private boolean fixFollowData(Long userId, Set<Long> redisFollowing, Set<Long> dbFollowing) {
        try {
            List<Long> toInsert = new ArrayList<>();
            if (redisFollowing != null) {
                for (Long followedId : redisFollowing) {
                    if (!dbFollowing.contains(followedId)) {
                        toInsert.add(followedId);
                    }
                }
            }

            List<Long> toDelete = new ArrayList<>();
            for (Long followedId : dbFollowing) {
                if (redisFollowing == null || !redisFollowing.contains(followedId)) {
                    toDelete.add(followedId);
                }
            }

            int fixedCount = 0;
            if (!toInsert.isEmpty()) {
                List<UserFollow> insertList = new ArrayList<>();
                for (Long followedId : toInsert) {
                    insertList.add(UserFollow.builder()
                            .followerId(userId)
                            .followedId(followedId)
                            .build());
                }
                fixedCount += followMapper.batchInsert(insertList);
            }

            if (!toDelete.isEmpty()) {
                List<UserFollow> deleteList = new ArrayList<>();
                for (Long followedId : toDelete) {
                    deleteList.add(UserFollow.builder()
                            .followerId(userId)
                            .followedId(followedId)
                            .build());
                }
                fixedCount += followMapper.batchDelete(deleteList);
            }

            if (fixedCount > 0) {
                log.info("Fixed {} follow data inconsistencies for userId: {}", fixedCount, userId);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to fix follow data for userId: {}", userId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public void validateBlockData(Long userId) {
        try {
            if (userId != null) {
                validateSingleUserBlock(userId);
            } else {
                log.warn("Full validation of block data not implemented, use specific userId");
            }
        } catch (Exception e) {
            log.error("Failed to validate block data for userId: {}", userId, e);
        }
    }

    /**
     * 校验单个用户的黑名单数据
     */
    private void validateSingleUserBlock(Long userId) {
        // 1. 从Redis读取黑名单列表
        Set<Long> redisBlacklist = boundUtil.sMembers(USER_BLACKLIST_PREFIX + userId, Long.class);
        long redisCount = redisBlacklist != null ? redisBlacklist.size() : 0;

        // 2. 从数据库读取黑名单列表
        List<Long> dbBlacklist = userBlockMapper.selectBlockedUserIds(userId);
        Set<Long> dbBlacklistSet = new HashSet<>(dbBlacklist);
        long dbCount = dbBlacklist.size();

        // 3. 比较差异
        long diffCount = Math.abs(redisCount - dbCount);
        if (diffCount == 0 && redisBlacklist != null && redisBlacklist.equals(dbBlacklistSet)) {
            log.debug("Block data consistent for userId: {}, count: {}", userId, redisCount);
            return;
        }

        // 4. 发现不一致，以Redis为准修复数据库
        log.warn("Block data inconsistent for userId: {}, redis: {}, db: {}, diff: {}",
                userId, redisCount, dbCount, diffCount);

        boolean fixed = fixBlockData(userId, redisBlacklist, dbBlacklistSet);
        ValidationResult result = new ValidationResult("BLOCK", userId, redisCount, dbCount,
                diffCount, fixed);
        log.info("Block data validation result: {}", result);
    }

    /**
     * 修复黑名单数据不一致
     */
    private boolean fixBlockData(Long userId, Set<Long> redisBlacklist, Set<Long> dbBlacklist) {
        try {
            List<Long> toInsert = new ArrayList<>();
            if (redisBlacklist != null) {
                for (Long blockedUserId : redisBlacklist) {
                    if (!dbBlacklist.contains(blockedUserId)) {
                        toInsert.add(blockedUserId);
                    }
                }
            }

            List<Long> toDelete = new ArrayList<>();
            for (Long blockedUserId : dbBlacklist) {
                if (redisBlacklist == null || !redisBlacklist.contains(blockedUserId)) {
                    toDelete.add(blockedUserId);
                }
            }

            int fixedCount = 0;
            if (!toInsert.isEmpty()) {
                long now = System.currentTimeMillis();
                List<UserBlock> insertList = new ArrayList<>();
                for (Long blockedUserId : toInsert) {
                    insertList.add(UserBlock.builder()
                            .userId(userId)
                            .blockedUserId(blockedUserId)
                            .createdAt(now)
                            .build());
                }
                fixedCount += userBlockMapper.batchInsert(insertList);
            }

            if (!toDelete.isEmpty()) {
                List<UserBlock> deleteList = new ArrayList<>();
                for (Long blockedUserId : toDelete) {
                    deleteList.add(UserBlock.builder()
                            .userId(userId)
                            .blockedUserId(blockedUserId)
                            .build());
                }
                fixedCount += userBlockMapper.batchDelete(deleteList);
            }

            if (fixedCount > 0) {
                log.info("Fixed {} block data inconsistencies for userId: {}", fixedCount, userId);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to fix block data for userId: {}", userId, e);
            return false;
        }
    }

    /**
     * 分批校验点赞数据
     */
    private void validateLikeDataInBatches() {
        try {
            List<Long> postIds = postLikeMapper.selectDistinctPostIdsWithPaging(
                    likeValidationOffset, batchValidationSize);

            if (postIds.isEmpty()) {
                // 重置偏移量，开始新一轮校验
                likeValidationOffset = 0;
                log.info("Like data validation cycle completed, resetting offset");
                return;
            }

            int validatedCount = 0;
            for (Long postId : postIds) {
                try {
                    validateLikeData(postId);
                    validatedCount++;
                } catch (Exception e) {
                    log.error("Failed to validate like data for postId: {}", postId, e);
                }
            }

            likeValidationOffset += postIds.size();
            log.info("Validated {} like records, current offset: {}", validatedCount, likeValidationOffset);
        } catch (Exception e) {
            log.error("Failed to validate like data in batches", e);
        }
    }

    /**
     * 分批校验收藏数据
     */
    private void validateFavoriteDataInBatches() {
        try {
            List<Long> userIds = postFavoriteMapper.selectDistinctUserIdsWithPaging(
                    favoriteValidationOffset, batchValidationSize);

            if (userIds.isEmpty()) {
                favoriteValidationOffset = 0;
                log.info("Favorite data validation cycle completed, resetting offset");
                return;
            }

            int validatedCount = 0;
            for (Long userId : userIds) {
                try {
                    validateFavoriteData(userId);
                    validatedCount++;
                } catch (Exception e) {
                    log.error("Failed to validate favorite data for userId: {}", userId, e);
                }
            }

            favoriteValidationOffset += userIds.size();
            log.info("Validated {} favorite records, current offset: {}", validatedCount, favoriteValidationOffset);
        } catch (Exception e) {
            log.error("Failed to validate favorite data in batches", e);
        }
    }

    /**
     * 分批校验关注数据
     */
    private void validateFollowDataInBatches() {
        try {
            List<Long> userIds = followMapper.selectDistinctUserIdsWithPaging(
                    followValidationOffset, batchValidationSize);

            if (userIds.isEmpty()) {
                followValidationOffset = 0;
                log.info("Follow data validation cycle completed, resetting offset");
                return;
            }

            int validatedCount = 0;
            for (Long userId : userIds) {
                try {
                    validateFollowData(userId);
                    validatedCount++;
                } catch (Exception e) {
                    log.error("Failed to validate follow data for userId: {}", userId, e);
                }
            }

            followValidationOffset += userIds.size();
            log.info("Validated {} follow records, current offset: {}", validatedCount, followValidationOffset);
        } catch (Exception e) {
            log.error("Failed to validate follow data in batches", e);
        }
    }

    /**
     * 分批校验黑名单数据
     */
    private void validateBlockDataInBatches() {
        try {
            List<Long> userIds = userBlockMapper.selectDistinctUserIdsWithPaging(
                    blockValidationOffset, batchValidationSize);

            if (userIds.isEmpty()) {
                blockValidationOffset = 0;
                log.info("Block data validation cycle completed, resetting offset");
                return;
            }

            int validatedCount = 0;
            for (Long userId : userIds) {
                try {
                    validateBlockData(userId);
                    validatedCount++;
                } catch (Exception e) {
                    log.error("Failed to validate block data for userId: {}", userId, e);
                }
            }

            blockValidationOffset += userIds.size();
            log.info("Validated {} block records, current offset: {}", validatedCount, blockValidationOffset);
        } catch (Exception e) {
            log.error("Failed to validate block data in batches", e);
        }
    }

    /**
     * 定时校验任务 - 业界常见做法
     * 
     * 定时校验策略：
     * 1. 固定间隔校验：每N分钟执行一次校验
     * 2. 分批校验：避免一次性校验所有数据，影响性能
     * 3. 抽样校验：对于大数据量，可以抽样校验
     * 4. 错峰执行：在业务低峰期执行，减少对业务的影响
     * 
     * 配置说明：
     * - fixedDelay: 上次执行完成后，延迟N毫秒再执行（适合长时间任务）
     * - fixedRate: 固定频率执行，不管上次是否完成（适合短时间任务）
     * - cron: 使用cron表达式，更灵活（如每天凌晨执行）
     * 
     * 可以通过 application.yml 配置：
     * relation:
     * validation:
     * interval: 1800000 # 30分钟
     * enabled: true # 是否启用定时校验
     */
    @Scheduled(fixedDelayString = "${relation.validation.interval:1800000}") // 默认30分钟
    public void scheduledValidation() {
        // 检查是否启用定时校验
        if (!validationEnabled) {
            log.debug("Scheduled validation is disabled, skipping");
            return;
        }

        try {
            log.info("Starting scheduled validation of relation data, type index: {}, batch size: {}",
                    validationTypeIndex, batchValidationSize);

            // 轮询校验不同类型的数据，避免一次性校验所有数据
            switch (validationTypeIndex % 4) {
                case 0:
                    log.info("Validating like data in batch");
                    validateLikeDataInBatches();
                    break;
                case 1:
                    log.info("Validating favorite data in batch");
                    validateFavoriteDataInBatches();
                    break;
                case 2:
                    log.info("Validating follow data in batch");
                    validateFollowDataInBatches();
                    break;
                case 3:
                    log.info("Validating block data in batch");
                    validateBlockDataInBatches();
                    break;
            }

            // 轮询到下一个类型
            validationTypeIndex = (validationTypeIndex + 1) % 4;

            log.info("Scheduled validation completed, next type index: {}", validationTypeIndex);

        } catch (Exception e) {
            log.error("Scheduled validation failed", e);
        }
    }

    /**
     * 定时修复任务 - 业界常见做法
     * 
     * 定时修复策略：
     * 1. 在定时校验后执行修复（校验过程中发现不一致会自动修复）
     * 2. 或者单独定时执行修复（修复最近发现的不一致数据）
     * 3. 修复时使用批量操作，提高效率
     * 4. 修复操作会修改数据库，需要谨慎执行
     * 
     * 注意：
     * - 修复操作以Redis为准，修复数据库
     * - 修复过程中如果Redis数据异常，需要人工介入
     * - 建议在业务低峰期执行修复操作
     * 
     * 配置说明：
     * relation:
     * validation:
     * fix:
     * interval: 3600000 # 1小时
     * initialDelay: 300000 # 初始延迟5分钟
     * enabled: true # 是否启用定时修复
     */
    @Scheduled(fixedDelayString = "${relation.validation.fix.interval:3600000}", initialDelayString = "${relation.validation.fix.initialDelay:300000}") // 默认1小时，初始延迟5分钟
    public void scheduledFix() {
        // 检查是否启用定时修复
        if (!fixEnabled) {
            log.debug("Scheduled fix is disabled, skipping");
            return;
        }

        try {
            log.info("Starting scheduled fix of relation data");

            // 业界常见做法：
            // 1. 定时校验过程中发现不一致会自动修复（在validateXXX方法中调用fixXXX方法）
            // 2. 这里可以执行额外的修复逻辑，比如：
            // - 修复最近发现的不一致数据（从校验结果队列中获取）
            // - 修复特定类型的数据
            // - 全量修复（谨慎使用，可能耗时较长）

            // 当前实现：校验过程中会自动修复不一致的数据
            // 如果需要额外的修复逻辑，可以在这里实现
            // 例如：从校验结果队列中获取需要修复的数据，批量修复

            // 示例：执行一次全量校验（会触发自动修复）
            // validateAllRelationData();

            log.info("Scheduled fix completed (fixes are performed during validation)");

        } catch (Exception e) {
            log.error("Scheduled fix failed", e);
        }
    }
}
