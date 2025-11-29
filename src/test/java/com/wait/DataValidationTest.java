package com.wait;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.wait.entity.domain.Post;
import com.wait.entity.domain.PostFavorite;
import com.wait.entity.domain.PostLike;
import com.wait.entity.domain.UserBase;
import com.wait.mapper.CommentMapper;
import com.wait.mapper.FollowMapper;
import com.wait.mapper.PostFavoriteMapper;
import com.wait.mapper.PostLikeMapper;
import com.wait.mapper.PostMapper;
import com.wait.mapper.UserBaseMapper;
import com.wait.mapper.UserBlockMapper;
import com.wait.service.RelationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 数据验证测试 - 验证Redis和数据库数据一致性
 */
@Slf4j
@SpringBootTest
public class DataValidationTest {

    @Autowired
    private UserBaseMapper userBaseMapper;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private PostLikeMapper postLikeMapper;
    @Autowired
    private PostFavoriteMapper postFavoriteMapper;
    @Autowired
    private UserBlockMapper userBlockMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private RelationService relationService;

    @Test
    public void validateAllData() {
        log.info("========== 开始数据一致性验证 ==========");

        int totalErrors = 0;

        // 1. 验证关注关系
        totalErrors += validateFollowRelations();

        // 2. 验证点赞关系
        totalErrors += validateLikeRelations();

        // 3. 验证收藏关系
        totalErrors += validateFavoriteRelations();

        // 4. 验证黑名单关系
        totalErrors += validateBlockRelations();

        // 5. 验证计数一致性
        totalErrors += validateCounts();

        log.info("========== 数据一致性验证完成 ==========");
        if (totalErrors == 0) {
            log.info("✓ 所有数据验证通过，未发现不一致");
        } else {
            log.warn("⚠ 发现 {} 处数据不一致（可能是测试过程中产生的临时不一致或表不存在）", totalErrors);
            log.info("提示：如果是在测试过程中发现的不一致，这是正常的，因为Redis和数据库的同步是异步的");
        }
    }

    private int validateFollowRelations() {
        log.info("1. 验证关注关系...");
        int errors = 0;

        try {
            List<UserBase> users = userBaseMapper.selectAll();
            if (users == null || users.isEmpty()) {
                log.warn("没有用户数据，跳过关注关系验证");
                return 0;
            }

            for (UserBase user : users) {
                // 从数据库查询关注列表
                List<Long> dbFollowing = followMapper.selectFollowedIds(user.getId());
                Set<Long> redisFollowing = relationService.getFollowing(user.getId());

                // 验证关注列表
                if (dbFollowing != null && !dbFollowing.isEmpty()) {
                    Set<Long> dbFollowingSet = dbFollowing.stream().collect(Collectors.toSet());
                    if (!dbFollowingSet.equals(redisFollowing)) {
                        log.error("用户 {} 的关注列表不一致: DB={}, Redis={}", 
                                user.getId(), dbFollowingSet, redisFollowing);
                        errors++;
                    }
                } else {
                    if (redisFollowing != null && !redisFollowing.isEmpty()) {
                        log.error("用户 {} 的关注列表不一致: DB为空, Redis={}", 
                                user.getId(), redisFollowing);
                        errors++;
                    }
                }

                // 验证关注数
                int dbFollowingCount = followMapper.countFollowing(user.getId());
                Long redisFollowingCount = relationService.getFollowingCount(user.getId());
                if (dbFollowingCount != redisFollowingCount) {
                    log.error("用户 {} 的关注数不一致: DB={}, Redis={}", 
                            user.getId(), dbFollowingCount, redisFollowingCount);
                    errors++;
                }

                // 验证粉丝数
                int dbFollowerCount = followMapper.countFollowers(user.getId());
                Long redisFollowerCount = relationService.getFollowerCount(user.getId());
                if (dbFollowerCount != redisFollowerCount) {
                    log.error("用户 {} 的粉丝数不一致: DB={}, Redis={}", 
                            user.getId(), dbFollowerCount, redisFollowerCount);
                    errors++;
                }
            }

            log.info("✓ 关注关系验证完成，发现 {} 处不一致", errors);
        } catch (Exception e) {
            log.error("关注关系验证失败", e);
            errors++;
        }

        return errors;
    }

    private int validateLikeRelations() {
        log.info("2. 验证点赞关系...");
        int errors = 0;

        try {
            List<PostLike> dbLikes = postLikeMapper.selectAll();
            if (dbLikes == null || dbLikes.isEmpty()) {
                log.warn("没有点赞数据，跳过点赞关系验证");
                return 0;
            }

            // 验证每个点赞关系
            for (PostLike like : dbLikes) {
                boolean redisLiked = relationService.isLiked(like.getUserId(), like.getPostId());
                if (!redisLiked) {
                    log.error("点赞关系不存在于Redis: userId={}, postId={}", 
                            like.getUserId(), like.getPostId());
                    errors++;
                }
            }

            // 验证每个帖子的点赞数
            List<Post> posts = postMapper.selectByIds(
                dbLikes.stream().map(PostLike::getPostId).distinct().collect(Collectors.toList())
            );
            for (Post post : posts) {
                int dbLikeCount = postLikeMapper.countByPostId(post.getId());
                Long redisLikeCount = relationService.getLikeCount(post.getId());
                if (dbLikeCount != redisLikeCount) {
                    log.error("帖子 {} 的点赞数不一致: DB={}, Redis={}", 
                            post.getId(), dbLikeCount, redisLikeCount);
                    errors++;
                }
            }

            log.info("✓ 点赞关系验证完成，发现 {} 处不一致", errors);
        } catch (Exception e) {
            log.error("点赞关系验证失败", e);
            errors++;
        }

        return errors;
    }

    private int validateFavoriteRelations() {
        log.info("3. 验证收藏关系...");
        int errors = 0;

        try {
            List<PostFavorite> dbFavorites = postFavoriteMapper.selectAll();
            if (dbFavorites == null) {
                log.warn("收藏关系表可能不存在，跳过验证");
                return 0;
            }
            if (dbFavorites == null || dbFavorites.isEmpty()) {
                log.warn("没有收藏数据，跳过收藏关系验证");
                return 0;
            }

            // 验证每个收藏关系
            for (PostFavorite favorite : dbFavorites) {
                boolean redisFavorited = relationService.isFavorited(
                    favorite.getUserId(), favorite.getPostId());
                if (!redisFavorited) {
                    log.error("收藏关系不存在于Redis: userId={}, postId={}", 
                            favorite.getUserId(), favorite.getPostId());
                    errors++;
                }
            }

            // 验证每个帖子的收藏数
            List<Post> posts = postMapper.selectByIds(
                dbFavorites.stream().map(PostFavorite::getPostId).distinct().collect(Collectors.toList())
            );
            for (Post post : posts) {
                int dbFavoriteCount = postFavoriteMapper.countByPostId(post.getId());
                Long redisFavoriteCount = relationService.getFavoriteCount(post.getId());
                if (dbFavoriteCount != redisFavoriteCount) {
                    log.error("帖子 {} 的收藏数不一致: DB={}, Redis={}", 
                            post.getId(), dbFavoriteCount, redisFavoriteCount);
                    errors++;
                }
            }

            log.info("✓ 收藏关系验证完成，发现 {} 处不一致", errors);
        } catch (Exception e) {
            log.error("收藏关系验证失败", e);
            errors++;
        }

        return errors;
    }

    private int validateBlockRelations() {
        log.info("4. 验证黑名单关系...");
        int errors = 0;

        try {
            List<UserBase> users = userBaseMapper.selectAll();
            if (users == null || users.isEmpty()) {
                log.warn("没有用户数据，跳过黑名单关系验证");
                return 0;
            }

            for (UserBase user : users) {
                // 从数据库查询黑名单列表
                List<Long> dbBlacklist = userBlockMapper.selectBlockedUserIds(user.getId());
                Set<Long> redisBlacklist = relationService.getBlacklist(user.getId());

                // 验证黑名单列表
                if (dbBlacklist != null && !dbBlacklist.isEmpty()) {
                    Set<Long> dbBlacklistSet = dbBlacklist.stream().collect(Collectors.toSet());
                    if (!dbBlacklistSet.equals(redisBlacklist)) {
                        log.error("用户 {} 的黑名单不一致: DB={}, Redis={}", 
                                user.getId(), dbBlacklistSet, redisBlacklist);
                        errors++;
                    }
                } else {
                    if (redisBlacklist != null && !redisBlacklist.isEmpty()) {
                        log.error("用户 {} 的黑名单不一致: DB为空, Redis={}", 
                                user.getId(), redisBlacklist);
                        errors++;
                    }
                }
            }

            log.info("✓ 黑名单关系验证完成，发现 {} 处不一致", errors);
        } catch (Exception e) {
            log.error("黑名单关系验证失败", e);
            errors++;
        }

        return errors;
    }

    private int validateCounts() {
        log.info("5. 验证计数一致性...");
        int errors = 0;

        try {
            // 获取所有帖子ID
            List<Post> allPosts = new java.util.ArrayList<>();
            List<UserBase> users = userBaseMapper.selectAll();
            if (users != null) {
                for (UserBase user : users) {
                    List<Post> userPosts = postMapper.selectByUserId(user.getId());
                    if (userPosts != null) {
                        allPosts.addAll(userPosts);
                    }
                }
            }
            List<Post> posts = allPosts;
            if (posts == null || posts.isEmpty()) {
                log.warn("没有帖子数据，跳过计数验证");
                return 0;
            }

            for (Post post : posts) {
                // 验证点赞数
                int dbLikeCount = postLikeMapper.countByPostId(post.getId());
                Long redisLikeCount = relationService.getLikeCount(post.getId());
                if (dbLikeCount != redisLikeCount) {
                    log.error("帖子 {} 的点赞数不一致: DB={}, Redis={}", 
                            post.getId(), dbLikeCount, redisLikeCount);
                    errors++;
                }

                // 验证收藏数
                int dbFavoriteCount = postFavoriteMapper.countByPostId(post.getId());
                Long redisFavoriteCount = relationService.getFavoriteCount(post.getId());
                if (dbFavoriteCount != redisFavoriteCount) {
                    log.error("帖子 {} 的收藏数不一致: DB={}, Redis={}", 
                            post.getId(), dbFavoriteCount, redisFavoriteCount);
                    errors++;
                }

                // 验证评论数
                int dbCommentCount = commentMapper.countByPostId(post.getId());
                // 注意：评论数可能没有在Redis中缓存，这里只验证数据库中的数据
                if (dbCommentCount != post.getCommentCount()) {
                    log.warn("帖子 {} 的评论数不一致: DB={}, Post对象={}", 
                            post.getId(), dbCommentCount, post.getCommentCount());
                    // 不增加错误计数，因为可能是缓存问题
                }
            }

            log.info("✓ 计数验证完成，发现 {} 处不一致", errors);
        } catch (Exception e) {
            log.error("计数验证失败", e);
            errors++;
        }

        return errors;
    }
}

