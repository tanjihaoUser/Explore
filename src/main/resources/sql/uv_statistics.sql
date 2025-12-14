-- UV 统计表
-- 用于持久化 Redis 中超过7天的 UV 统计数据
CREATE TABLE IF NOT EXISTS `uv_statistics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `resource_type` VARCHAR(50) NOT NULL COMMENT '资源类型（post, article, page等）',
    `resource_id` BIGINT NOT NULL COMMENT '资源ID',
    `date` VARCHAR(8) NOT NULL COMMENT '日期（格式：yyyyMMdd）',
    `visitor_id` VARCHAR(100) NOT NULL COMMENT '访客ID（用户ID或IP地址）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_resource_date_visitor` (`resource_type`, `resource_id`, `date`, `visitor_id`),
    KEY `idx_resource_type_id` (`resource_type`, `resource_id`),
    KEY `idx_resource_date` (`resource_type`, `resource_id`, `date`),
    KEY `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UV统计表';

-- 索引说明：
-- uk_resource_date_visitor: 唯一索引，防止同一资源在同一天重复记录同一访客（自动去重）
-- idx_resource_type_id: 用于按资源类型和ID查询UV
-- idx_resource_date: 用于按资源类型、ID和日期查询UV（复合索引，优化查询性能）
-- idx_date: 用于按日期查询和清理过期数据

-- ============================================
-- Mock 数据：UV 统计
-- ============================================
-- 注意：date 使用 yyyyMMdd 格式，created_at 和 updated_at 由数据库自动处理
-- 这些数据都是超过7天的历史数据，用于测试持久化功能

-- Mock Data for uv_statistics (8-10 days ago)
INSERT INTO `uv_statistics` (`resource_type`, `resource_id`, `date`, `visitor_id`) VALUES
('post', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '1'),
('post', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '2'),
('post', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '3'),
('post', 2, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '1'),
('post', 2, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '4'),
('post', 3, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 9 DAY), '%Y%m%d'), '2'),
('post', 3, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 9 DAY), '%Y%m%d'), '5'),
('post', 4, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 9 DAY), '%Y%m%d'), '3'),
('post', 5, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 10 DAY), '%Y%m%d'), '1'),
('post', 5, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 10 DAY), '%Y%m%d'), '6'),
('page', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '1'),
('page', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '2'),
('page', 2, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 9 DAY), '%Y%m%d'), '3'),
('page', 2, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 9 DAY), '%Y%m%d'), '4'),
('user_profile', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '2'),
('user_profile', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '5'),
('user_profile', 2, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 9 DAY), '%Y%m%d'), '1'),
('user_profile', 2, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 9 DAY), '%Y%m%d'), '6'),
('article', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '1'),
('article', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '3'),
('article', 2, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 9 DAY), '%Y%m%d'), '2'),
('article', 2, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 9 DAY), '%Y%m%d'), '4'),
('category', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '1'),
('category', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 8 DAY), '%Y%m%d'), '5'),
('search', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 9 DAY), '%Y%m%d'), '2'),
('search', 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 9 DAY), '%Y%m%d'), '6');

