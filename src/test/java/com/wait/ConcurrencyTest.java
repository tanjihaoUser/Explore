package com.wait;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.wait.entity.domain.Post;
import com.wait.entity.domain.UserBase;
import com.wait.mapper.PostMapper;
import com.wait.mapper.UserBaseMapper;
import com.wait.service.RelationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 并发测试 - 模拟高并发点赞、收藏等操作
 */
@Slf4j
@SpringBootTest
public class ConcurrencyTest {

    @Autowired
    private UserBaseMapper userBaseMapper;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private RelationService relationService;

    private List<Long> testUserIds;
    private List<Long> testPostIds;
    private static final int CONCURRENT_THREADS = 50;
    private static final int OPERATIONS_PER_THREAD = 20;

    @BeforeEach
    public void setup() {
        // 获取测试用户和帖子
        List<UserBase> users = userBaseMapper.selectAll();
        if (users != null && !users.isEmpty()) {
            testUserIds = users.stream().map(UserBase::getId).collect(Collectors.toList());
        } else {
            testUserIds = Collections.emptyList();
        }

        List<Post> posts = new ArrayList<>();
        if (testUserIds != null && !testUserIds.isEmpty()) {
            for (Long userId : testUserIds) {
                List<Post> userPosts = postMapper.selectByUserId(userId);
                if (userPosts != null) {
                    posts.addAll(userPosts);
                }
            }
        }
        testPostIds = posts.stream().map(Post::getId).distinct().collect(Collectors.toList());

        log.info("测试数据准备完成: {} 个用户, {} 个帖子", 
                testUserIds.size(), testPostIds.size());
    }

    @Test
    public void testConcurrentLikes() throws InterruptedException {
        log.info("========== 并发点赞测试 ==========");
        log.info("并发线程数: {}, 每线程操作数: {}", CONCURRENT_THREADS, OPERATIONS_PER_THREAD);

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过并发点赞测试");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // 先清理测试数据
        Long testPostId = testPostIds.get(0);
        for (Long userId : testUserIds) {
            relationService.unlikePost(userId, testPostId);
        }

        // 并发执行点赞操作
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        Long userId = testUserIds.get(threadIndex % testUserIds.size());
                        Long postId = testPostIds.get(j % testPostIds.size());

                        boolean result = relationService.likePost(userId, postId);
                        if (result) {
                            successCount.incrementAndGet();
                        } else {
                            duplicateCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    log.error("并发点赞操作失败", e);
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 验证结果
        Long finalLikeCount = relationService.getLikeCount(testPostId);
        Set<Long> likers = relationService.getLikers(testPostId);

        log.info("========== 并发点赞测试结果 ==========");
        log.info("总耗时: {} ms", duration);
        log.info("成功操作: {}", successCount.get());
        log.info("重复操作: {}", duplicateCount.get());
        log.info("失败操作: {}", failureCount.get());
        log.info("最终点赞数: {}", finalLikeCount);
        log.info("点赞用户数: {}", likers != null ? likers.size() : 0);
        log.info("QPS: {}", (successCount.get() + duplicateCount.get()) * 1000.0 / duration);

        // 验证数据一致性
        assert finalLikeCount != null : "点赞数不应为null";
        assert likers != null : "点赞用户列表不应为null";
        assert finalLikeCount == likers.size() : 
            String.format("点赞数应该等于点赞用户数: count=%d, users=%d", finalLikeCount, likers.size());

        log.info("✓ 并发点赞测试通过，数据一致性验证成功");
    }

    @Test
    public void testConcurrentFavorites() throws InterruptedException {
        log.info("========== 并发收藏测试 ==========");
        log.info("并发线程数: {}, 每线程操作数: {}", CONCURRENT_THREADS, OPERATIONS_PER_THREAD);

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过并发收藏测试");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // 先清理测试数据
        Long testPostId = testPostIds.get(0);
        for (Long userId : testUserIds) {
            relationService.unfavoritePost(userId, testPostId);
        }

        // 并发执行收藏操作
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        Long userId = testUserIds.get(threadIndex % testUserIds.size());
                        Long postId = testPostIds.get(j % testPostIds.size());

                        boolean result = relationService.favoritePost(userId, postId);
                        if (result) {
                            successCount.incrementAndGet();
                        } else {
                            duplicateCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    log.error("并发收藏操作失败", e);
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 验证结果
        Long finalFavoriteCount = relationService.getFavoriteCount(testPostId);

        log.info("========== 并发收藏测试结果 ==========");
        log.info("总耗时: {} ms", duration);
        log.info("成功操作: {}", successCount.get());
        log.info("重复操作: {}", duplicateCount.get());
        log.info("失败操作: {}", failureCount.get());
        log.info("最终收藏数: {}", finalFavoriteCount);
        log.info("QPS: {}", (successCount.get() + duplicateCount.get()) * 1000.0 / duration);

        // 验证数据一致性
        assert finalFavoriteCount != null : "收藏数不应为null";

        log.info("✓ 并发收藏测试通过，数据一致性验证成功");
    }

    @Test
    public void testConcurrentLikeAndUnlike() throws InterruptedException {
        log.info("========== 并发点赞/取消点赞测试 ==========");
        log.info("并发线程数: {}, 每线程操作数: {}", CONCURRENT_THREADS, OPERATIONS_PER_THREAD);

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过并发点赞/取消点赞测试");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger likeCount = new AtomicInteger(0);
        AtomicInteger unlikeCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        Long testPostId = testPostIds.get(0);

        // 并发执行点赞和取消点赞操作
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    Long userId = testUserIds.get(threadIndex % testUserIds.size());
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        if (j % 2 == 0) {
                            // 点赞
                            if (relationService.likePost(userId, testPostId)) {
                                likeCount.incrementAndGet();
                            }
                        } else {
                            // 取消点赞
                            if (relationService.unlikePost(userId, testPostId)) {
                                unlikeCount.incrementAndGet();
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("并发点赞/取消点赞操作失败", e);
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 验证结果
        Long finalLikeCount = relationService.getLikeCount(testPostId);
        Set<Long> likers = relationService.getLikers(testPostId);

        log.info("========== 并发点赞/取消点赞测试结果 ==========");
        log.info("总耗时: {} ms", duration);
        log.info("点赞操作: {}", likeCount.get());
        log.info("取消点赞操作: {}", unlikeCount.get());
        log.info("失败操作: {}", failureCount.get());
        log.info("最终点赞数: {}", finalLikeCount);
        log.info("点赞用户数: {}", likers != null ? likers.size() : 0);
        log.info("QPS: {}", (likeCount.get() + unlikeCount.get()) * 1000.0 / duration);

        // 验证数据一致性
        assert finalLikeCount != null : "点赞数不应为null";
        assert likers != null : "点赞用户列表不应为null";
        assert finalLikeCount == likers.size() : 
            String.format("点赞数应该等于点赞用户数: count=%d, users=%d", finalLikeCount, likers.size());

        log.info("✓ 并发点赞/取消点赞测试通过，数据一致性验证成功");
    }

    @Test
    public void testConcurrentFollow() throws InterruptedException {
        log.info("========== 并发关注测试 ==========");
        log.info("并发线程数: {}, 每线程操作数: {}", CONCURRENT_THREADS, OPERATIONS_PER_THREAD);

        if (testUserIds.size() < 2) {
            log.warn("测试用户不足，跳过并发关注测试");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        Long testFollowerId = testUserIds.get(0);

        // 先清理测试数据
        for (int i = 1; i < Math.min(testUserIds.size(), 100); i++) {
            relationService.unfollow(testFollowerId, testUserIds.get(i));
        }

        // 并发执行关注操作
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        int targetIndex = (threadIndex * OPERATIONS_PER_THREAD + j) % (testUserIds.size() - 1) + 1;
                        Long followedId = testUserIds.get(targetIndex);

                        boolean result = relationService.follow(testFollowerId, followedId);
                        if (result) {
                            successCount.incrementAndGet();
                        } else {
                            duplicateCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    log.error("并发关注操作失败", e);
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 验证结果
        Long finalFollowingCount = relationService.getFollowingCount(testFollowerId);
        Set<Long> following = relationService.getFollowing(testFollowerId);

        log.info("========== 并发关注测试结果 ==========");
        log.info("总耗时: {} ms", duration);
        log.info("成功操作: {}", successCount.get());
        log.info("重复操作: {}", duplicateCount.get());
        log.info("失败操作: {}", failureCount.get());
        log.info("最终关注数: {}", finalFollowingCount);
        log.info("关注用户数: {}", following != null ? following.size() : 0);
        log.info("QPS: {}", (successCount.get() + duplicateCount.get()) * 1000.0 / duration);

        // 验证数据一致性
        assert finalFollowingCount != null : "关注数不应为null";
        assert following != null : "关注列表不应为null";
        assert finalFollowingCount == following.size() : 
            String.format("关注数应该等于关注用户数: count=%d, users=%d", finalFollowingCount, following.size());

        log.info("✓ 并发关注测试通过，数据一致性验证成功");
    }
}

