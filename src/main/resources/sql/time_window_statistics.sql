-- 时间窗口统计数据表
-- 用于持久化 Redis 中超过7天的统计数据
CREATE TABLE IF NOT EXISTS `time_window_statistics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `metric` VARCHAR(100) NOT NULL COMMENT '指标名称（如：visit_count, sales_amount等）',
    `value` VARCHAR(500) NOT NULL COMMENT '数据值（存储为字符串，支持数字和文本）',
    `timestamp` BIGINT NOT NULL COMMENT '时间戳（毫秒）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_metric_timestamp` (`metric`, `timestamp`),
    KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间窗口统计数据表';

-- 索引说明：
-- idx_metric_timestamp: 用于按指标和时间范围查询
-- idx_timestamp: 用于按时间范围查询和清理过期数据

-- ============================================
-- Mock 数据：时间窗口统计数据
-- ============================================
-- 注意：timestamp 使用时间戳（毫秒），created_at 和 updated_at 由数据库自动处理
-- 这些数据都是超过7天的历史数据，用于测试持久化功能

LOCK TABLES `time_window_statistics` WRITE;
/*!40000 ALTER TABLE `time_window_statistics` DISABLE KEYS */;

-- 帖子浏览量统计（post:view:postId）
INSERT INTO `time_window_statistics` (`metric`, `value`, `timestamp`) VALUES
-- 8天前的数据
('post:view:1', '150', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000),
('post:view:2', '230', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 1000),
('post:view:3', '180', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 2000),
('post:view:4', '95', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 3000),
('post:view:5', '320', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 4000),
-- 9天前的数据
('post:view:1', '145', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000),
('post:view:2', '225', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 1000),
('post:view:6', '110', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 2000),
('post:view:7', '280', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 3000),
('post:view:8', '165', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 4000),
-- 10天前的数据
('post:view:1', '140', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000),
('post:view:9', '200', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000 + 1000),
('post:view:10', '350', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000 + 2000);

-- 首页浏览量统计（homepage:view）
INSERT INTO `time_window_statistics` (`metric`, `value`, `timestamp`) VALUES
('homepage:view', '1250', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000),
('homepage:view', '1180', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 3600000),
('homepage:view', '1320', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 7200000),
('homepage:view', '1100', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000),
('homepage:view', '1280', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 3600000),
('homepage:view', '1150', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000);

-- 帖子点赞统计（post:like:postId）
INSERT INTO `time_window_statistics` (`metric`, `value`, `timestamp`) VALUES
('post:like:1', '25', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000),
('post:like:2', '38', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 1000),
('post:like:3', '22', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 2000),
('post:like:4', '15', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 3000),
('post:like:5', '45', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 4000),
('post:like:6', '18', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000),
('post:like:7', '52', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 1000),
('post:like:8', '28', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 2000),
('post:like:9', '20', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000),
('post:like:10', '65', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000 + 1000);

-- 帖子收藏统计（post:favorite:postId）
INSERT INTO `time_window_statistics` (`metric`, `value`, `timestamp`) VALUES
('post:favorite:1', '12', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000),
('post:favorite:2', '18', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 1000),
('post:favorite:3', '10', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 2000),
('post:favorite:4', '8', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 3000),
('post:favorite:5', '22', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 4000),
('post:favorite:6', '9', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000),
('post:favorite:7', '28', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 1000),
('post:favorite:8', '15', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 2000),
('post:favorite:9', '11', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000),
('post:favorite:10', '35', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000 + 1000);

-- API请求统计（api:request）
INSERT INTO `time_window_statistics` (`metric`, `value`, `timestamp`) VALUES
('api:request', '8500', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000),
('api:request', '9200', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 3600000),
('api:request', '8800', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 7200000),
('api:request', '8100', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000),
('api:request', '9500', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 3600000),
('api:request', '8700', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000);

-- 用户活动统计（user:activity）
INSERT INTO `time_window_statistics` (`metric`, `value`, `timestamp`) VALUES
('user:activity', '450', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000),
('user:activity', '520', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 3600000),
('user:activity', '480', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 7200000),
('user:activity', '410', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000),
('user:activity', '550', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 3600000),
('user:activity', '470', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000);

-- 错误统计（error:count）
INSERT INTO `time_window_statistics` (`metric`, `value`, `timestamp`) VALUES
('error:count', '5', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000),
('error:count', '3', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 3600000),
('error:count', '8', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 7200000),
('error:count', '2', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000),
('error:count', '6', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 3600000),
('error:count', '4', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000);

-- 页面浏览量统计（page:view）
INSERT INTO `time_window_statistics` (`metric`, `value`, `timestamp`) VALUES
('page:view', '3200', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000),
('page:view', '3500', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 3600000),
('page:view', '3100', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 7200000),
('page:view', '2900', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000),
('page:view', '3600', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 3600000),
('page:view', '3300', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000);

-- 搜索查询统计（search:query）
INSERT INTO `time_window_statistics` (`metric`, `value`, `timestamp`) VALUES
('search:query', '180', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000),
('search:query', '220', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 3600000),
('search:query', '195', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 7200000),
('search:query', '160', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000),
('search:query', '240', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 3600000),
('search:query', '200', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000);

-- 评论统计（comment:count:postId）
INSERT INTO `time_window_statistics` (`metric`, `value`, `timestamp`) VALUES
('comment:count:1', '8', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000),
('comment:count:2', '15', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 1000),
('comment:count:3', '12', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 2000),
('comment:count:4', '5', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 3000),
('comment:count:5', '20', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY)) * 1000 + 4000),
('comment:count:6', '18', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000),
('comment:count:7', '25', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 1000),
('comment:count:8', '10', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000 + 2000),
('comment:count:9', '7', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000),
('comment:count:10', '30', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000 + 1000);

/*!40000 ALTER TABLE `time_window_statistics` ENABLE KEYS */;
UNLOCK TABLES;

