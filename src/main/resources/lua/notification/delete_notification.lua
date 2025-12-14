-- delete_notification.lua
-- 原子性地删除通知
-- KEYS[1]: 通知列表 key (notification:user:{userId})
-- KEYS[2]: 未读通知集合 key (notification:unread:{userId})
-- ARGV[1]: notificationId (通知ID)
--
-- 原子性地执行：
-- 1. 从通知列表中移除（LREM）
-- 2. 从未读通知集合中移除（SREM）

local deleted = 0

-- 1. 从通知列表中移除（LREM count value：移除count个值为value的元素）
-- 由于通知格式是 notificationId:type:content:relatedId，需要查找以notificationId开头的通知
local notifications = redis.call('LRANGE', KEYS[1], 0, -1)
if notifications then
    for i, notification in ipairs(notifications) do
        -- 检查通知是否以notificationId开头
        local prefix = ARGV[1] .. ':'
        if string.sub(notification, 1, string.len(prefix)) == prefix then
            -- 找到匹配的通知，删除它（只删除第一个匹配的）
            redis.call('LREM', KEYS[1], 1, notification)
            deleted = 1
            break
        end
    end
end

-- 2. 从未读通知集合中移除
local removed = redis.call('SREM', KEYS[2], ARGV[1])

-- 返回删除结果（1表示成功删除，0表示未找到）
if deleted == 1 or removed > 0 then
    return 1
else
    return 0
end

