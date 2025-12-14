-- record_browse.lua
-- KEYS[1]: 浏览记录 key (browse:history:user:{userId})
-- ARGV[1]: postId (帖子ID)
-- ARGV[2]: currentTime (当前时间戳，毫秒)
-- ARGV[3]: maxRecords (最大记录数，默认1000)
--
-- 原子性地执行：
-- 1. 添加浏览记录（ZADD，如果已存在则更新时间）
-- 2. 检查记录数量（ZCARD）
-- 3. 如果超过限制，删除最旧的记录（ZREMRANGEBYRANK）
--
-- 返回：删除的记录数量（如果没有删除则返回0）

-- 1. 添加浏览记录（如果已存在则更新时间）
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])

-- 2. 检查记录数量
local size = redis.call('ZCARD', KEYS[1])
local maxSize = tonumber(ARGV[3])

-- 3. 如果超过限制，删除最旧的记录
if size > maxSize then
    -- 删除排名从0到(size - maxSize - 1)的记录（保留最新的maxSize条）
    local removedCount = size - maxSize
    redis.call('ZREMRANGEBYRANK', KEYS[1], 0, removedCount - 1)
    return removedCount
end

-- 返回0表示没有删除记录
return 0

