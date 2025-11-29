package com.wait.entity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    private Long id;
    private Long userId;
    private String content;
    private Integer likeCount;
    private Integer commentCount;
    private Integer isDeleted;
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
    
    // 以下字段不持久化到数据库，仅用于API响应
    private Boolean isLiked;      // 当前用户是否已点赞
    private Boolean isFavorited;  // 当前用户是否已收藏
    private Integer favoriteCount; // 收藏数（从Redis获取）
    private String username;       // 用户名（从UserBase获取）

}
