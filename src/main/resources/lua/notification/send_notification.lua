-- send_notification.lua
-- 原子性地发送通知并添加到未读集合
-- KEYS[1]: 通知列表 key (notification:user:{userId})
-- KEYS[2]: 未读通知集合 key (notification:unread:{userId})
-- ARGV[1]: notificationId (通知ID)
-- ARGV[2]: notification (通知内容，格式：notificationId:type:content:relatedId)
-- ARGV[3]: maxNotifications (最大通知数量，用于LTRIM)
--
-- 原子性地执行：
-- 1. 将通知添加到列表头部（LPUSH）
-- 2. 添加到未读通知集合（SADD）
-- 3. 限制通知数量（LTRIM）

-- 1. 将通知添加到列表头部
redis.call('LPUSH', KEYS[1], ARGV[2])

-- 2. 添加到未读通知集合
redis.call('SADD', KEYS[2], ARGV[1])

-- 3. 限制通知数量，防止无限增长
local maxSize = tonumber(ARGV[3])
if maxSize > 0 then
    local listSize = redis.call('LLEN', KEYS[1])
    if listSize > maxSize then
        -- 保留前maxSize个元素，删除后面的元素
        -- LTRIM key start end: 保留索引从start到end的元素（包含两端）
        -- 索引从0开始，所以保留0到maxSize-1
        redis.call('LTRIM', KEYS[1], 0, maxSize - 1)
    end
end

-- 返回通知ID
return ARGV[1]

