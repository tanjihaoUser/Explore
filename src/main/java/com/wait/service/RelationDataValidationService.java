package com.wait.service;

/**
 * 关系数据校验服务接口
 * 用于定时校验Redis和数据库的一致性，确保数据同步
 */
public interface RelationDataValidationService {

    /**
     * 校验点赞数据一致性
     * 比较Redis中的点赞关系和数据库中的点赞关系，修复不一致的数据
     * 
     * @param postId 帖子ID，如果为null则校验所有帖子
     */
    void validateLikeData(Long postId);

    /**
     * 校验收藏数据一致性
     * 比较Redis中的收藏关系和数据库中的收藏关系，修复不一致的数据
     * 
     * @param userId 用户ID，如果为null则校验所有用户
     */
    void validateFavoriteData(Long userId);

    /**
     * 校验关注数据一致性
     * 比较Redis中的关注关系和数据库中的关注关系，修复不一致的数据
     * 
     * @param userId 用户ID，如果为null则校验所有用户
     */
    void validateFollowData(Long userId);

    /**
     * 校验黑名单数据一致性
     * 比较Redis中的黑名单关系和数据库中的黑名单关系，修复不一致的数据
     * 
     * @param userId 用户ID，如果为null则校验所有用户
     */
    void validateBlockData(Long userId);

}
