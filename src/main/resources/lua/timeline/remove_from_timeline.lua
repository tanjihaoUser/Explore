-- remove_from_timeline.lua
-- KEYS[1]: user timeline key (timeline:posts:user:{userId})
-- KEYS[2]: global timeline key (timeline:posts:global)
-- ARGV[1]: postId
--
-- 原子性地执行：
-- 1. 从用户时间线移除帖子
-- 2. 从全局时间线移除帖子
--
-- 返回：总共移除的元素数量（最多为2，最少为0）

local removedCount = 0

-- 1. 从用户时间线移除
local removedFromUser = redis.call('ZREM', KEYS[1], ARGV[1])
if removedFromUser == 1 then
    removedCount = removedCount + 1
end

-- 2. 从全局时间线移除
local removedFromGlobal = redis.call('ZREM', KEYS[2], ARGV[1])
if removedFromGlobal == 1 then
    removedCount = removedCount + 1
end

return removedCount

