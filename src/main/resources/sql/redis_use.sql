# ************************************************************
# Sequel Ace SQL dump
# 版本号： 20087
#
# https://sequel-ace.com/
# https://github.com/Sequel-Ace/Sequel-Ace
#
# 主机: localhost (MySQL 8.0.42)
# 数据库: redis_use
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# 转储表 post
# ------------------------------------------------------------

DROP TABLE IF EXISTS `post`;

CREATE TABLE `post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '发布用户ID',
  `content` text NOT NULL COMMENT '帖子内容',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `comment_count` int NOT NULL DEFAULT '0' COMMENT '评论数',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0-正常 1-删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_deleted` (`is_deleted`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子表';

LOCK TABLES `post` WRITE;
/*!40000 ALTER TABLE `post` DISABLE KEYS */;

INSERT INTO `post` (`id`, `user_id`, `content`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`)
VALUES
	(1,1,'今天天气真好，适合出去散步！',5,3,0,'2025-11-10 10:17:33','2025-11-10 10:17:33'),
	(2,2,'刚刚完成了一个大项目，庆祝一下！',12,8,0,'2025-11-10 10:17:33','2025-11-10 10:17:33'),
	(3,3,'分享一首好听的音乐给大家',8,5,0,'2025-11-10 10:17:33','2025-11-10 10:17:33'),
	(4,1,'学习Redis的时间线功能，很有意思',3,2,0,'2025-11-10 10:17:33','2025-11-10 10:17:33'),
	(5,4,'新买的相机到了，拍了几张照片',15,10,0,'2025-11-10 10:17:33','2025-11-10 10:17:33'),
	(6,2,'周末去哪里玩比较好呢？求推荐',7,15,0,'2025-11-10 10:17:33','2025-11-10 10:17:33'),
	(7,5,'健身打卡第30天，继续坚持！',20,12,0,'2025-11-10 10:17:33','2025-11-10 10:17:33'),
	(8,3,'读了一本好书，推荐给大家《Redis实战》',6,4,0,'2025-11-10 10:17:33','2025-11-10 10:17:33'),
	(9,1,'今天学会了如何使用Redis List',4,3,0,'2025-11-10 10:17:33','2025-11-10 10:17:33'),
	(10,4,'美食分享：自制意大利面',25,18,0,'2025-11-10 10:17:33','2025-11-10 10:17:33'),
	(11,1,'略显焦虑的一天',0,0,0,'2025-11-12 09:57:38','2025-11-12 09:57:38'),
	(12,1,'略显焦虑的一天',0,0,1,'2025-11-12 10:02:38','2025-11-12 10:02:38'),
	(13,1,'略显焦虑的一天~~',0,0,1,'2025-11-12 10:06:38','2025-11-12 10:06:38'),
	(14,1,'使用Redis list的一天',0,0,1,'2025-11-12 10:53:29','2025-11-26 10:03:06'),
	(15,1,'使用gemini解答哲学问题',0,0,0,'2025-11-12 15:27:06','2025-11-12 15:27:06'),
	(16,1,'chatgpt初试',0,0,0,'2025-11-14 13:57:06','2025-11-14 13:57:06'),
	(17,2,'发工资啦，又要开始定投了',0,0,0,'2025-11-15 17:32:06','2025-11-15 17:32:06'),
	(18,2,'test list function',0,0,1,'2025-11-16 10:22:23','2025-11-16 10:22:23'),
	(19,2,'test list trim function',0,0,1,'2025-11-16 10:32:06','2025-11-26 10:00:06'),
	(20,2,'lalala......',0,0,0,'2025-11-16 14:32:06','2025-11-26 09:59:33'),
	(21,3,'测试Redis中时间线功能',0,0,0,'2025-11-26 10:14:49','2025-11-26 10:14:49'),
	(22,3,'锦瑟无端五十弦，一弦一柱思华年',0,0,0,'2025-11-26 10:15:32','2025-11-26 10:15:32'),
	(23,3,'这周的一些重大事件：周五计划去酒吧喝酒，周六加班，连上13天',0,0,0,'2025-11-26 10:16:06','2025-11-26 10:16:06'),
	(24,4,'成功利用agent写代码的一些伟大尝试',0,0,0,'2025-11-26 10:20:00','2025-11-26 10:20:00'),
	(25,4,'agent确实好用，最近没怎么写过代码了',0,0,0,'2025-11-26 10:20:18','2025-11-26 10:20:18'),
	(26,4,'感慨个人的局限，AI的万能，人在时代洪流中的渺小',0,0,0,'2025-11-26 10:20:53','2025-11-26 10:20:53'),
	(27,5,'继续收听理财知识',0,0,0,'2025-11-26 10:21:10','2025-11-26 10:21:10'),
	(28,5,'两个重要观点，猎豹应该花时间捕捉羚羊，而不是田鼠。得不偿失，人生也是如此\n我们应该创新，而不是随大流。有时甚至可以刻意打破常规，反而有不一样的收获',0,0,0,'2025-11-26 10:22:50','2025-11-26 10:22:50'),
	(29,5,'波动是正常的，但不一定代表风险，可能代表更高收益',0,0,0,'2025-11-26 10:23:23','2025-11-26 10:23:23'),
	(30,5,'有耐心，跟时间做朋友，复利思维会给我们带来好处',0,0,0,'2025-11-26 10:23:49','2025-11-26 10:23:49'),
	(31,1001,'使用大模型写的前端好丑啊，页面不兼容',0,0,0,'2025-11-26 17:45:00','2025-11-26 17:45:00'),
	(32,1001,'使用大模型写的前端好丑啊，页面不兼容',0,0,0,'2025-11-26 17:45:23','2025-11-26 17:45:23'),
	(33,1001,'# test\n\n**test**\n## title\n`code`\n```java\npublic class Main{}\n```\n*smile*\n---\n',0,0,0,'2025-11-26 20:10:54','2025-11-26 20:10:54'),
	(34,1001,'# test\n\n**test**\n## title\n`code`\n```java\npublic class Main{}\n```\n*smile*\n---\n',0,0,0,'2025-11-26 20:11:04','2025-11-26 20:11:04'),
	(35,1001,'# AI初试\n\n使用AI生成代码的一些趣事和反思\n- 上午尝试使用cursor生成Redis项目的前端，发现很快能生成出来 \n- 当时觉得前端已死，现在回想，有些荒谬，也没验证一下，也是没有时间\n- 抱着这份思想左脚踩右脚，生成后端\n- 发现十分高效，用到了Redis集群，微服务架构，甚至DockerFile都出来了\n- 觉得后端也要完蛋，有些失落，寻求gemini做了心理按摩\n- 开始调试前端，发现全是报错，需要几轮交互才调试的差不多，觉得人们还有机会\n- 另一方面，自己可能高估了AI的能力，补充和辅助开发一个功能或许可以，从无到有创建项目还是费劲\n- 另一方面，借AI意识到实际生产中环境复杂得多，甚至还有跨环境调用，AI一时可能难以取代\n**光怪陆离的一天，中午看着AI快速生成后端代码的时候，心情跌至谷底，随后不断反思，又从谷底反弹**\n有些遗憾，今天都没怎么写工作代码，被AI震惊到了，沉溺于自己的幻想中',0,0,0,'2025-11-26 22:55:36','2025-11-26 22:55:36'),
	(36,1001,'# title\n\ntest\nredis\nand vue',0,0,0,'2025-11-27 10:11:27','2025-11-27 10:11:27');

/*!40000 ALTER TABLE `post` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 post_comment
# ------------------------------------------------------------

DROP TABLE IF EXISTS `post_comment`;

CREATE TABLE `post_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID（用于回复功能，NULL表示顶级评论）',
  `content` text NOT NULL COMMENT '评论内容',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `is_deleted` tinyint DEFAULT '0' COMMENT '是否删除（0-未删除，1-已删除）',
  `created_at` bigint NOT NULL COMMENT '创建时间（时间戳，毫秒）',
  `updated_at` bigint DEFAULT NULL COMMENT '更新时间（时间戳，毫秒）',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_post_created` (`post_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子评论表';

LOCK TABLES `post_comment` WRITE;
/*!40000 ALTER TABLE `post_comment` DISABLE KEYS */;

INSERT INTO `post_comment` (`id`, `post_id`, `user_id`, `parent_id`, `content`, `like_count`, `is_deleted`, `created_at`, `updated_at`)
VALUES
	(1,1,2,NULL,'天气确实不错，我也想去散步！',3,0,1699600000000,NULL),
	(2,1,3,NULL,'同感，今天阳光很好',2,0,1699600100000,NULL),
	(3,1,4,1,'一起啊，我也想去',1,0,1699600200000,NULL),
	(4,2,1,NULL,'恭喜恭喜！',5,0,1699600300000,NULL),
	(5,2,3,NULL,'太厉害了，值得庆祝！',4,0,1699600400000,NULL),
	(6,2,5,4,'谢谢！',2,0,1699600500000,NULL),
	(7,3,1,NULL,'什么音乐？分享一下',3,0,1699600600000,NULL),
	(8,3,2,NULL,'我也喜欢音乐',2,0,1699600700000,NULL),
	(9,4,2,NULL,'Redis确实很有意思',1,0,1699600800000,NULL),
	(10,5,1,NULL,'照片拍得真好',4,0,1699600900000,NULL),
	(11,5,2,NULL,'什么相机？',3,0,1699601000000,NULL),
	(12,5,3,11,'佳能EOS R5',2,0,1699601100000,NULL),
	(13,6,1,NULL,'推荐去公园',2,0,1699601200000,NULL),
	(14,6,3,NULL,'或者去海边',1,0,1699601300000,NULL),
	(15,7,1,NULL,'坚持就是胜利！',6,0,1699601400000,NULL),
	(16,7,2,NULL,'我也在健身，一起加油',5,0,1699601500000,NULL),
	(17,8,1,NULL,'这本书我也在看',3,0,1699601600000,NULL),
	(18,9,2,NULL,'Redis List功能很强大',2,0,1699601700000,NULL),
	(19,10,1,NULL,'看起来很好吃',7,0,1699601800000,NULL),
	(20,10,2,NULL,'求教程',6,0,1699601900000,NULL),
	(21,15,2,NULL,'AI确实改变了开发方式',4,0,1700000000000,NULL),
	(22,15,3,NULL,'但还是要理解原理',3,0,1700000100000,NULL),
	(23,16,1,NULL,'ChatGPT很好用',2,0,1700100000000,NULL),
	(24,17,3,NULL,'定投是个好习惯',5,0,1700200000000,NULL),
	(25,20,1,NULL,'lalala',1,0,1700300000000,NULL),
	(26,21,1,NULL,'时间线功能测试',2,0,1700400000000,NULL),
	(27,22,2,NULL,'好诗！',3,0,1700500000000,NULL),
	(28,23,1,NULL,'连上13天太辛苦了',4,0,1700600000000,NULL),
	(29,24,2,NULL,'Agent确实好用',3,0,1700700000000,NULL),
	(30,25,1,NULL,'同感，最近也在用',2,0,1700800000000,NULL),
	(31,26,3,NULL,'深有感触',5,0,1700900000000,NULL),
	(32,27,1,NULL,'理财知识很重要',4,0,1701000000000,NULL),
	(33,28,2,NULL,'很有道理',3,0,1701100000000,NULL),
	(34,29,1,NULL,'风险和收益并存',2,0,1701200000000,NULL),
	(35,30,2,NULL,'时间是最好的朋友',4,0,1701300000000,NULL),
	(36,33,1,NULL,'Markdown测试',1,0,1701400000000,NULL),
	(37,35,2,NULL,'AI确实改变了开发',6,0,1701500000000,NULL),
	(38,35,3,NULL,'但还是要理解原理',5,0,1701600000000,NULL),
	(39,35,4,37,'同意，AI是工具',4,0,1701700000000,NULL),
	(40,36,1,NULL,'测试评论',1,0,1701800000000,NULL);

/*!40000 ALTER TABLE `post_comment` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 post_favorite
# ------------------------------------------------------------

DROP TABLE IF EXISTS `post_favorite`;

CREATE TABLE `post_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '收藏用户ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `created_at` bigint NOT NULL COMMENT '收藏时间（时间戳，毫秒）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post` (`user_id`,`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子收藏表';

LOCK TABLES `post_favorite` WRITE;
/*!40000 ALTER TABLE `post_favorite` DISABLE KEYS */;

INSERT INTO `post_favorite` (`id`, `user_id`, `post_id`, `created_at`)
VALUES
	(1,1,2,1699600000000),
	(2,1,5,1699600100000),
	(3,1,7,1699600200000),
	(4,1,10,1699600300000),
	(5,2,1,1699600400000),
	(6,2,3,1699600500000),
	(7,2,8,1699600600000),
	(8,2,15,1699600700000),
	(9,3,2,1699600800000),
	(10,3,4,1699600900000),
	(11,3,6,1699601000000),
	(12,3,9,1699601100000),
	(13,4,1,1699601200000),
	(14,4,5,1699601300000),
	(15,4,7,1699601400000),
	(16,4,10,1699601500000),
	(17,5,2,1699601600000),
	(18,5,6,1699601700000),
	(19,5,8,1699601800000),
	(20,5,15,1699601900000),
	(21,6,1,1699602000000),
	(22,6,3,1699602100000),
	(23,6,5,1699602200000),
	(24,6,7,1699602300000),
	(25,7,2,1699602400000),
	(26,7,4,1699602500000),
	(27,7,6,1699602600000),
	(28,7,10,1699602700000),
	(29,8,1,1699602800000),
	(30,8,5,1699602900000),
	(31,8,8,1699603000000),
	(32,8,15,1699603100000),
	(33,9,2,1699603200000),
	(34,9,7,1699603300000),
	(35,9,10,1699603400000),
	(36,10,1,1699603500000),
	(37,10,3,1699603600000),
	(38,10,5,1699603700000),
	(39,10,8,1699603800000),
	(40,1,15,1700000000000),
	(41,1,16,1700100000000),
	(42,1,20,1700300000000),
	(43,1,22,1700500000000),
	(44,1,24,1700700000000),
	(45,1,26,1700900000000),
	(46,1,28,1701100000000),
	(47,1,30,1701300000000),
	(48,1,35,1701500000000),
	(49,2,17,1700200000000),
	(50,2,21,1700400000000),
	(51,2,23,1700600000000),
	(52,2,25,1700800000000),
	(53,2,27,1701000000000),
	(54,2,29,1701200000000),
	(55,2,35,1701600000000),
	(56,2,36,1701800000000),
	(57,3,20,1700300000000),
	(58,3,22,1700500000000),
	(59,3,24,1700700000000),
	(60,3,26,1700900000000),
	(61,4,15,1700000000000),
	(62,4,17,1700200000000),
	(63,4,21,1700400000000),
	(64,4,23,1700600000000),
	(65,5,20,1700300000000),
	(66,5,25,1700800000000),
	(67,5,28,1701100000000),
	(68,5,30,1701300000000),
	(69,6,16,1700100000000),
	(70,6,22,1700500000000),
	(71,6,27,1701000000000),
	(72,6,29,1701200000000),
	(73,7,15,1700000000000),
	(74,7,21,1700400000000),
	(75,7,26,1700900000000),
	(76,7,35,1701500000000),
	(77,8,17,1700200000000),
	(78,8,23,1700600000000),
	(79,8,28,1701100000000),
	(80,8,36,1701800000000),
	(81,9,20,1700300000000),
	(82,9,24,1700700000000),
	(83,9,30,1701300000000),
	(84,9,35,1701600000000),
	(85,10,16,1700100000000),
	(86,10,22,1700500000000),
	(87,10,25,1700800000000),
	(88,10,29,1701200000000);

/*!40000 ALTER TABLE `post_favorite` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 post_like
# ------------------------------------------------------------

DROP TABLE IF EXISTS `post_like`;

CREATE TABLE `post_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '点赞用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（点赞时间）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子点赞表';

LOCK TABLES `post_like` WRITE;
/*!40000 ALTER TABLE `post_like` DISABLE KEYS */;

INSERT INTO `post_like` (`id`, `post_id`, `user_id`, `created_at`)
VALUES
	(1,1,2,'2025-11-25 14:32:06'),
	(2,1,3,'2025-11-25 14:32:06'),
	(3,1,4,'2025-11-25 14:32:06'),
	(4,1,5,'2025-11-25 14:32:06'),
	(5,1,6,'2025-11-25 14:32:06'),
	(6,2,1,'2025-11-25 14:32:06'),
	(7,2,3,'2025-11-25 14:32:06'),
	(8,2,5,'2025-11-25 14:32:06'),
	(9,3,1,'2025-11-25 14:32:06'),
	(10,3,2,'2025-11-25 14:32:06'),
	(11,4,2,'2025-11-25 14:32:06'),
	(12,3,4,'2025-11-25 14:32:06'),
	(13,4,3,'2025-11-25 14:32:06'),
	(14,5,1,'2025-11-25 14:32:06'),
	(15,5,2,'2025-11-25 14:32:06'),
	(16,5,3,'2025-11-25 14:32:06'),
	(19,2,2,'2025-11-27 14:26:53'),
	(23,22,2,'2025-11-29 15:25:32'),
	(225,16,1,'2025-11-29 17:02:57'),
	(226,16,7,'2025-11-29 17:02:57'),
	(236,16,2,'2025-11-29 17:02:57'),
	(237,16,9,'2025-11-29 17:02:57'),
	(238,16,8,'2025-11-29 17:02:57'),
	(239,16,6,'2025-11-29 17:02:57'),
	(241,16,4,'2025-11-29 17:02:58'),
	(242,15,8,'2025-11-29 17:02:58'),
	(243,15,7,'2025-11-29 17:02:58'),
	(244,15,6,'2025-11-29 17:02:58'),
	(245,15,2,'2025-11-29 17:02:58'),
	(247,15,9,'2025-11-29 17:02:58'),
	(248,15,5,'2025-11-29 17:02:58'),
	(249,15,4,'2025-11-29 17:02:58'),
	(250,15,1,'2025-11-29 17:02:58'),
	(251,15,10,'2025-11-29 17:02:58'),
	(252,11,1,'2025-11-29 17:02:58'),
	(253,11,7,'2025-11-29 17:02:58'),
	(254,11,6,'2025-11-29 17:02:58'),
	(255,11,4,'2025-11-29 17:02:58'),
	(256,11,2,'2025-11-29 17:02:58'),
	(257,9,2,'2025-11-29 17:02:58'),
	(258,9,4,'2025-11-29 17:02:58'),
	(259,9,5,'2025-11-29 17:02:58'),
	(260,9,6,'2025-11-29 17:02:58'),
	(261,9,8,'2025-11-29 17:02:58'),
	(262,9,9,'2025-11-29 17:02:58'),
	(263,11,10,'2025-11-29 17:02:58'),
	(264,9,1,'2025-11-29 17:02:58'),
	(265,11,9,'2025-11-29 17:02:58'),
	(266,11,8,'2025-11-29 17:02:58'),
	(267,11,5,'2025-11-29 17:02:58'),
	(268,4,9,'2025-11-29 17:02:58'),
	(269,9,7,'2025-11-29 17:02:58'),
	(270,9,10,'2025-11-29 17:02:58'),
	(271,4,10,'2025-11-29 17:02:58'),
	(272,4,4,'2025-11-29 17:02:58'),
	(273,4,6,'2025-11-29 17:02:58'),
	(274,4,7,'2025-11-29 17:02:58'),
	(275,4,8,'2025-11-29 17:02:58'),
	(276,4,1,'2025-11-29 17:02:58'),
	(277,4,5,'2025-11-29 17:02:58'),
	(278,6,5,'2025-11-29 17:02:58'),
	(279,17,4,'2025-11-29 17:02:58'),
	(280,20,8,'2025-11-29 17:02:58'),
	(281,20,7,'2025-11-29 17:02:58'),
	(282,6,2,'2025-11-29 17:02:58'),
	(283,20,2,'2025-11-29 17:02:58'),
	(284,1,1,'2025-11-29 17:02:58'),
	(285,20,6,'2025-11-29 17:02:58'),
	(286,20,5,'2025-11-29 17:02:58'),
	(287,20,4,'2025-11-29 17:02:58'),
	(288,1,9,'2025-11-29 17:02:58'),
	(289,17,6,'2025-11-29 17:02:58'),
	(290,17,2,'2025-11-29 17:02:58'),
	(291,1,7,'2025-11-29 17:02:58'),
	(292,1,8,'2025-11-29 17:02:58'),
	(293,20,1,'2025-11-29 17:02:58'),
	(294,17,5,'2025-11-29 17:02:58'),
	(295,20,9,'2025-11-29 17:02:58'),
	(296,20,10,'2025-11-29 17:02:58'),
	(297,1,10,'2025-11-29 17:02:58'),
	(298,6,9,'2025-11-29 17:02:58'),
	(299,6,10,'2025-11-29 17:02:58'),
	(300,2,4,'2025-11-29 17:02:58'),
	(301,6,1,'2025-11-29 17:02:58'),
	(302,2,6,'2025-11-29 17:02:58'),
	(303,26,5,'2025-11-29 17:02:58'),
	(304,17,1,'2025-11-29 17:02:58'),
	(305,26,2,'2025-11-29 17:02:58'),
	(306,6,6,'2025-11-29 17:02:58'),
	(307,17,9,'2025-11-29 17:02:58'),
	(308,17,7,'2025-11-29 17:02:58'),
	(309,6,4,'2025-11-29 17:02:58'),
	(310,25,2,'2025-11-29 17:02:58'),
	(311,17,8,'2025-11-29 17:02:58'),
	(312,6,8,'2025-11-29 17:02:58'),
	(313,17,10,'2025-11-29 17:02:58'),
	(314,2,8,'2025-11-29 17:02:58'),
	(315,2,9,'2025-11-29 17:02:58'),
	(316,6,7,'2025-11-29 17:02:58'),
	(317,26,6,'2025-11-29 17:02:58'),
	(318,25,5,'2025-11-29 17:02:58'),
	(319,26,8,'2025-11-29 17:02:58'),
	(320,25,6,'2025-11-29 17:02:58'),
	(321,26,4,'2025-11-29 17:02:58'),
	(322,26,1,'2025-11-29 17:02:58'),
	(323,2,7,'2025-11-29 17:02:58'),
	(324,2,10,'2025-11-29 17:02:58'),
	(325,25,4,'2025-11-29 17:02:58'),
	(326,24,5,'2025-11-29 17:02:58'),
	(327,25,1,'2025-11-29 17:02:58'),
	(328,24,2,'2025-11-29 17:02:58'),
	(329,10,2,'2025-11-29 17:02:58'),
	(330,26,9,'2025-11-29 17:02:58'),
	(331,26,7,'2025-11-29 17:02:58'),
	(332,24,6,'2025-11-29 17:02:58'),
	(333,26,10,'2025-11-29 17:02:58'),
	(334,10,5,'2025-11-29 17:02:58'),
	(335,25,9,'2025-11-29 17:02:58'),
	(336,25,8,'2025-11-29 17:02:58'),
	(337,24,4,'2025-11-29 17:02:58'),
	(338,24,1,'2025-11-29 17:02:58'),
	(339,5,5,'2025-11-29 17:02:58'),
	(340,30,2,'2025-11-29 17:02:58'),
	(341,25,7,'2025-11-29 17:02:58'),
	(342,10,6,'2025-11-29 17:02:58'),
	(343,25,10,'2025-11-29 17:02:58'),
	(344,10,1,'2025-11-29 17:02:58'),
	(345,5,6,'2025-11-29 17:02:58'),
	(346,24,9,'2025-11-29 17:02:58'),
	(347,24,8,'2025-11-29 17:02:58'),
	(348,10,4,'2025-11-29 17:02:58'),
	(349,30,5,'2025-11-29 17:02:58'),
	(350,29,5,'2025-11-29 17:02:58'),
	(351,24,7,'2025-11-29 17:02:58'),
	(352,29,2,'2025-11-29 17:02:58'),
	(353,10,8,'2025-11-29 17:02:58'),
	(354,24,10,'2025-11-29 17:02:58'),
	(355,30,1,'2025-11-29 17:02:58'),
	(356,5,8,'2025-11-29 17:02:58'),
	(357,10,9,'2025-11-29 17:02:58'),
	(358,28,2,'2025-11-29 17:02:58'),
	(359,5,4,'2025-11-29 17:02:58'),
	(360,30,4,'2025-11-29 17:02:58'),
	(361,27,2,'2025-11-29 17:02:58'),
	(362,10,7,'2025-11-29 17:02:58'),
	(363,30,6,'2025-11-29 17:02:58'),
	(364,10,10,'2025-11-29 17:02:58'),
	(365,5,9,'2025-11-29 17:02:58'),
	(366,5,7,'2025-11-29 17:02:58'),
	(367,29,6,'2025-11-29 17:02:58'),
	(368,28,5,'2025-11-29 17:02:58'),
	(369,29,4,'2025-11-29 17:02:58'),
	(370,29,1,'2025-11-29 17:02:58'),
	(371,28,6,'2025-11-29 17:02:58'),
	(372,28,4,'2025-11-29 17:02:58'),
	(373,27,5,'2025-11-29 17:02:58'),
	(374,28,1,'2025-11-29 17:02:58'),
	(375,30,9,'2025-11-29 17:02:58'),
	(376,30,8,'2025-11-29 17:02:58'),
	(377,5,10,'2025-11-29 17:02:58'),
	(378,27,6,'2025-11-29 17:02:58'),
	(379,30,10,'2025-11-29 17:02:58'),
	(380,27,1,'2025-11-29 17:02:58'),
	(381,30,7,'2025-11-29 17:02:58'),
	(382,7,2,'2025-11-29 17:02:58'),
	(383,29,9,'2025-11-29 17:02:58'),
	(384,29,8,'2025-11-29 17:02:58'),
	(385,7,5,'2025-11-29 17:02:58'),
	(386,29,7,'2025-11-29 17:02:58'),
	(387,27,4,'2025-11-29 17:02:58'),
	(388,7,4,'2025-11-29 17:02:58'),
	(389,27,9,'2025-11-29 17:02:58'),
	(390,28,7,'2025-11-29 17:02:58'),
	(391,27,8,'2025-11-29 17:02:58'),
	(392,28,10,'2025-11-29 17:02:58'),
	(393,28,9,'2025-11-29 17:02:58'),
	(394,28,8,'2025-11-29 17:02:58'),
	(395,7,6,'2025-11-29 17:02:58'),
	(396,29,10,'2025-11-29 17:02:58'),
	(397,7,1,'2025-11-29 17:02:58'),
	(398,7,7,'2025-11-29 17:02:58'),
	(399,27,7,'2025-11-29 17:02:58'),
	(400,7,8,'2025-11-29 17:02:58'),
	(401,7,9,'2025-11-29 17:02:58'),
	(402,27,10,'2025-11-29 17:02:58'),
	(403,7,10,'2025-11-29 17:03:27');

/*!40000 ALTER TABLE `post_like` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 user_base
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_base`;

CREATE TABLE `user_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) NOT NULL COMMENT '邮箱',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希值（BCrypt加密）',
  `salt` varchar(64) DEFAULT NULL COMMENT '密码盐值（可选，BCrypt自带盐值）',
  `password_update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '密码最后更新时间',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用 1-正常 2-冻结',
  `user_type` tinyint(1) NOT NULL DEFAULT '1' COMMENT '用户类型：1-普通用户 2-VIP用户 3-管理员',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_password_update_time` (`password_update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基础信息表';

LOCK TABLES `user_base` WRITE;
/*!40000 ALTER TABLE `user_base` DISABLE KEYS */;

INSERT INTO `user_base` (`id`, `username`, `email`, `password_hash`, `salt`, `password_update_time`, `phone`, `status`, `user_type`, `create_time`, `update_time`, `last_login_time`)
VALUES
	(1,'zhangsan','zhangsan@example.com','$2a$10$nHWJQULYx7.WnK11Qjdp/uSm4/205tEALmRQcLtfIkTVZ.A87IHjG',NULL,'2025-11-29 11:08:02','13800138001',1,1,'2025-10-13 10:55:15','2025-11-29 11:08:01','2025-11-27 11:59:02'),
	(2,'lizhujin','lisi@example.com','$2a$10$nHWJQULYx7.WnK11Qjdp/uSm4/205tEALmRQcLtfIkTVZ.A87IHjG',NULL,'2025-11-29 11:08:02','1234567890',1,2,'2025-10-13 10:55:15','2025-11-29 15:33:47','2025-11-29 15:33:47'),
	(3,'yanhengzhi','wangwu@example.com','$2a$10$N.zmdr9k7uOCQb0bta/OauRwzhCp/nkWaoCvtJ2BO0laorO.5TlVG',NULL,'2025-11-27 10:24:48','13800138003',0,1,'2025-10-13 10:55:15','2025-11-27 13:51:21',NULL),
	(4,'user1','user1@example.com','$2a$10$nHWJQULYx7.WnK11Qjdp/uSm4/205tEALmRQcLtfIkTVZ.A87IHjG',NULL,'2025-11-29 11:08:02','13800138001',1,1,'2025-11-10 09:53:24','2025-11-29 11:08:01',NULL),
	(5,'user2','user2@example.com','$2a$10$nHWJQULYx7.WnK11Qjdp/uSm4/205tEALmRQcLtfIkTVZ.A87IHjG',NULL,'2025-11-29 11:08:02','13800138002',1,1,'2025-11-10 09:53:24','2025-11-29 11:08:01',NULL),
	(6,'user3','user3@example.com','$2a$10$nHWJQULYx7.WnK11Qjdp/uSm4/205tEALmRQcLtfIkTVZ.A87IHjG',NULL,'2025-11-29 11:08:02','13800138003',1,1,'2025-11-10 09:53:24','2025-11-29 11:08:01',NULL),
	(7,'user4','user4@example.com','$2a$10$nHWJQULYx7.WnK11Qjdp/uSm4/205tEALmRQcLtfIkTVZ.A87IHjG',NULL,'2025-11-29 11:08:02','13800138004',1,1,'2025-11-10 09:53:24','2025-11-29 11:08:01',NULL),
	(8,'user5','user5@example.com','$2a$10$nHWJQULYx7.WnK11Qjdp/uSm4/205tEALmRQcLtfIkTVZ.A87IHjG',NULL,'2025-11-29 11:08:02','13800138005',1,1,'2025-11-10 09:53:24','2025-11-29 11:08:01',NULL),
	(9,'admin','admin@example.com','$2a$10$nHWJQULYx7.WnK11Qjdp/uSm4/205tEALmRQcLtfIkTVZ.A87IHjG',NULL,'2025-11-29 11:08:02','13800000000',1,3,'2025-11-27 11:36:33','2025-11-29 11:08:01','2025-11-27 13:41:44'),
	(10,'testuser','test@example.com','$2a$10$nHWJQULYx7.WnK11Qjdp/uSm4/205tEALmRQcLtfIkTVZ.A87IHjG',NULL,'2025-11-29 11:08:02','13800000001',1,1,'2025-11-27 11:36:33','2025-11-29 11:08:01',NULL);

/*!40000 ALTER TABLE `user_base` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 user_block
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_block`;

CREATE TABLE `user_block` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID（拉黑者）',
  `blocked_user_id` bigint NOT NULL COMMENT '被拉黑的用户ID',
  `created_at` bigint NOT NULL COMMENT '拉黑时间（时间戳，毫秒）',
  `updated_at` bigint DEFAULT NULL COMMENT '更新时间（时间戳，毫秒）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_blocked` (`user_id`,`blocked_user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_blocked_user_id` (`blocked_user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户黑名单表';

LOCK TABLES `user_block` WRITE;
/*!40000 ALTER TABLE `user_block` DISABLE KEYS */;

INSERT INTO `user_block` (`id`, `user_id`, `blocked_user_id`, `created_at`, `updated_at`)
VALUES
	(1,1,3,1699600000000,NULL),
	(2,2,5,1699600100000,NULL),
	(3,3,1,1699600200000,NULL),
	(4,4,2,1699600300000,NULL),
	(5,5,4,1699600400000,NULL);

/*!40000 ALTER TABLE `user_block` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 user_detail
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_detail`;

CREATE TABLE `user_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `gender` tinyint(1) DEFAULT '0' COMMENT '性别：0-未知 1-男 2-女',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `signature` varchar(200) DEFAULT NULL COMMENT '个性签名',
  `country` varchar(50) DEFAULT '中国' COMMENT '国家',
  `province` varchar(50) DEFAULT NULL COMMENT '省份',
  `city` varchar(50) DEFAULT NULL COMMENT '城市',
  `address` varchar(200) DEFAULT NULL COMMENT '详细地址',
  `postal_code` varchar(10) DEFAULT NULL COMMENT '邮政编码',
  `preferences` json DEFAULT NULL COMMENT '用户偏好设置',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_gender` (`gender`),
  KEY `idx_city` (`city`),
  CONSTRAINT `fk_user_detail_base` FOREIGN KEY (`user_id`) REFERENCES `user_base` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户详情信息表';

LOCK TABLES `user_detail` WRITE;
/*!40000 ALTER TABLE `user_detail` DISABLE KEYS */;

INSERT INTO `user_detail` (`id`, `user_id`, `real_name`, `gender`, `birthday`, `avatar`, `signature`, `country`, `province`, `city`, `address`, `postal_code`, `preferences`, `create_time`, `update_time`)
VALUES
	(1,1,'张三',1,'1990-05-15','/avatars/1.jpg','热爱编程的程序员','中国','北京市','北京市',NULL,NULL,'{\"theme\": \"dark\", \"language\": \"zh-CN\", \"notifications\": true}','2025-10-13 10:55:15','2025-10-13 10:55:15'),
	(2,2,'李四',2,'1988-12-20','/avatars/2.jpg','喜欢旅行的设计师','中国','上海市','上海市',NULL,NULL,'{\"theme\": \"light\", \"language\": \"en-US\", \"notifications\": false}','2025-10-13 10:55:15','2025-10-13 10:55:15'),
	(3,3,'王五',1,'1995-08-10','/avatars/3.jpg','健身爱好者','中国','广东省','深圳市',NULL,NULL,'{\"theme\": \"auto\", \"language\": \"zh-CN\", \"notifications\": true}','2025-10-13 10:55:15','2025-10-13 10:55:15');

/*!40000 ALTER TABLE `user_detail` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 user_follow
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_follow`;

CREATE TABLE `user_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `follower_id` bigint NOT NULL COMMENT '关注者ID',
  `followed_id` bigint NOT NULL COMMENT '被关注者ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（关注时间）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_followed` (`follower_id`,`followed_id`),
  KEY `idx_follower_id` (`follower_id`),
  KEY `idx_followed_id` (`followed_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注关系表';

LOCK TABLES `user_follow` WRITE;
/*!40000 ALTER TABLE `user_follow` DISABLE KEYS */;

INSERT INTO `user_follow` (`id`, `follower_id`, `followed_id`, `created_at`)
VALUES
	(2,1,3,'2025-11-25 14:32:06'),
	(4,2,1,'2025-11-25 14:32:06'),
	(5,2,3,'2025-11-25 14:32:06'),
	(6,2,5,'2025-11-25 14:32:06'),
	(7,3,5,'2025-11-25 14:32:06'),
	(9,4,1,'2025-11-25 14:32:06'),
	(10,5,2,'2025-11-25 14:32:06'),
	(13,1,4,'2025-11-29 17:02:58'),
	(14,1,8,'2025-11-29 17:02:58'),
	(15,1,5,'2025-11-29 17:02:58'),
	(16,1,2,'2025-11-29 17:02:58'),
	(17,1,7,'2025-11-29 17:02:58'),
	(18,1,9,'2025-11-29 17:02:58'),
	(19,1,6,'2025-11-29 17:02:58'),
	(20,1,10,'2025-11-29 17:02:58');

/*!40000 ALTER TABLE `user_follow` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 user_session
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_session`;

CREATE TABLE `user_session` (
  `session_id` varchar(64) NOT NULL COMMENT '会话ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(255) NOT NULL COMMENT '用户名',
  `last_active_time` bigint NOT NULL COMMENT '最后活跃时间戳',
  `visit_count` int NOT NULL DEFAULT '0' COMMENT '访问次数',
  `current_page` varchar(500) DEFAULT '/' COMMENT '当前页面路径',
  `theme` varchar(50) DEFAULT 'light' COMMENT '主题配色',
  `language` varchar(10) DEFAULT 'zh-CN' COMMENT '语言偏好',
  `attributes` json DEFAULT NULL COMMENT '扩展属性(JSON格式)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_last_active_time` (`last_active_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户会话信息表';

LOCK TABLES `user_session` WRITE;
/*!40000 ALTER TABLE `user_session` DISABLE KEYS */;

INSERT INTO `user_session` (`session_id`, `user_id`, `username`, `last_active_time`, `visit_count`, `current_page`, `theme`, `language`, `attributes`, `create_time`, `update_time`)
VALUES
	('sess_1761704302664_374',1001,'yanhengzhi',1761704302664,1,'/home','light','zh-CN','{}','2025-10-29 10:18:22','2025-10-29 10:18:22'),
	('sess_1761705183934_313',1001,'yanhengzhi',1761705183935,1,'/home','light','zh-CN','{}','2025-10-29 10:33:04','2025-10-29 10:33:04'),
	('sess_1761705864517_95',1001,'yanhengzhi',1761705864517,1,'/home','light','zh-CN','{}','2025-10-29 10:44:24','2025-10-29 10:44:24'),
	('sess_1762084459228_577',1001,'william.tan',1762084530873,5,'/index','light','en','{}','2025-11-02 19:54:19','2025-11-02 21:59:50'),
	('sess_1762584907201_1075',1001,'mike.fu',1762584954589,3,'/2025','orange','jp','{}','2025-11-08 14:55:07','2025-11-08 14:59:09'),
	('sess_1764150221080_22',1001,'yanhengzhi',1764160194456,48,'/search','light','zh-CN','{}','2025-11-26 17:43:41','2025-11-26 20:31:40'),
	('sess_1764164286711_8609',1001,'yanhengzhi',1764169048852,69,'/home','light','zh-CN','{}','2025-11-26 21:38:07','2025-11-26 22:57:36'),
	('sess_1764209371227_2257',1001,'admin',1764210182369,17,'/home','light','zh-CN','{}','2025-11-27 10:09:31','2025-11-27 10:24:43'),
	('sess_1764215907727_7722',9,'admin',1764215907727,1,'/home','light','zh-CN','{}','2025-11-27 11:58:27','2025-11-27 11:58:27'),
	('sess_1764215942149_5460',1,'zhangsan',1764215942149,1,'/home','light','zh-CN','{}','2025-11-27 11:59:02','2025-11-27 11:59:02'),
	('sess_1764215948043_8311',2,'lizhujin',1764215948043,1,'/home','light','zh-CN','{}','2025-11-27 11:59:08','2025-11-27 11:59:08'),
	('sess_1764221771187_2605',2,'lizhujin',1764221771187,1,'/home','light','zh-CN','{}','2025-11-27 13:36:11','2025-11-27 13:36:11'),
	('sess_1764221894178_6002',2,'lizhujin',1764221894178,1,'/home','light','zh-CN','{}','2025-11-27 13:38:14','2025-11-27 13:38:14'),
	('sess_1764222104202_3944',9,'admin',1764222124146,2,'/home','light','zh-CN','{}','2025-11-27 13:41:44','2025-11-27 13:44:04'),
	('sess_1764222718492_6477',2,'lizhujin',1764339986747,135,'/home','light','zh-CN','{}','2025-11-27 13:51:58','2025-11-28 22:27:12'),
	('sess_1764397981297_2266',2,'lizhujin',1764397981298,1,'/home','light','zh-CN','{}','2025-11-29 14:33:01','2025-11-29 14:33:01'),
	('sess_1764399481967_9093',2,'lizhujin',1764399481967,1,'/home','light','zh-CN','{}','2025-11-29 14:58:01','2025-11-29 14:58:01'),
	('sess_1764401627079_8828',2,'lizhujin',1764401627079,1,'/home','light','zh-CN','{}','2025-11-29 15:33:47','2025-11-29 15:33:47');

/*!40000 ALTER TABLE `user_session` ENABLE KEYS */;
UNLOCK TABLES;