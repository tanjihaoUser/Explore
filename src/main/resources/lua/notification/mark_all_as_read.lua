-- mark_all_as_read.lua
-- 原子性地标记所有通知为已读
-- KEYS[1]: 未读通知集合 key (notification:unread:{userId})
--
-- 原子性地执行：
-- 1. 获取所有未读通知ID（SMEMBERS）
-- 2. 删除所有未读通知（DEL 或 SREM所有成员）
--
-- 返回：标记为已读的通知数量

-- 1. 获取未读通知数量（SCARD比SMEMBERS更高效）
local count = redis.call('SCARD', KEYS[1])

if count == 0 then
    -- 没有未读通知
    return 0
end

-- 2. 删除整个未读通知集合（DEL比逐个SREM更高效）
redis.call('DEL', KEYS[1])

-- 返回标记为已读的数量
return count

