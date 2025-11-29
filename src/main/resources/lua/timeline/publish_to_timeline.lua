-- publish_to_timeline.lua
-- KEYS[1]: user timeline key (timeline:posts:user:{userId})
-- KEYS[2]: global timeline key (timeline:posts:global)
-- ARGV[1]: postId
-- ARGV[2]: publishTime (时间戳，毫秒)
-- ARGV[3]: maxCachedPosts (最多缓存帖子数，默认1000)
--
-- 原子性地执行：
-- 1. 添加帖子到用户时间线 Sorted Set
-- 2. 添加帖子到全局时间线 Sorted Set
-- 3. 限制用户时间线大小（只保留最新的N条）

-- 1. 添加到用户时间线
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])

-- 2. 添加到全局时间线
redis.call('ZADD', KEYS[2], ARGV[2], ARGV[1])

-- 3. 限制用户时间线大小
local maxSize = tonumber(ARGV[3])
local userTimelineSize = redis.call('ZCARD', KEYS[1])
if userTimelineSize > maxSize then
    -- 删除排名 maxSize 之后的所有帖子（从索引 maxSize 到末尾）
    redis.call('ZREMRANGEBYRANK', KEYS[1], maxSize, -1)
    local removedCount = userTimelineSize - maxSize
    return removedCount
end

-- 返回0表示没有删除元素
return 0

