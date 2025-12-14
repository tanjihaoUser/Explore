-- recommend_and_mark.lua
-- KEYS[1]: 候选用户集合 key (recommend:candidate:{userId})
-- KEYS[2]: 已推荐用户集合 key (recommend:shown:{userId})
-- ARGV[1]: count (需要推荐的数量)
--
-- 原子性地执行：
-- 1. 从候选池中随机弹出指定数量的用户（SPOP）
-- 2. 将弹出的用户添加到已推荐集合（SADD）
--
-- 返回：推荐的用户ID列表（JSON数组格式的字符串）

local candidateKey = KEYS[1]
local recommendedKey = KEYS[2]
local count = tonumber(ARGV[1])

if count <= 0 then
    return cjson.encode({})
end

local recommended = {}

-- 循环执行 SPOP，直到达到指定数量或候选池为空
for i = 1, count do
    local userId = redis.call('SPOP', candidateKey)
    if userId == false or userId == nil then
        -- 候选池已空，退出循环
        break
    end
    
    -- 添加到结果列表
    table.insert(recommended, userId)
    
    -- 标记为已推荐（添加到已推荐集合）
    redis.call('SADD', recommendedKey, userId)
end

-- 返回推荐的用户ID列表（JSON数组）
return cjson.encode(recommended)

