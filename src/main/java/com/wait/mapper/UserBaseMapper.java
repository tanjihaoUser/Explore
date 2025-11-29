package com.wait.mapper;

import com.wait.annotation.RedisCache;
import com.wait.entity.domain.UserBase;
import com.wait.entity.type.DataOperationType;
import com.wait.entity.type.ReadStrategyType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserBaseMapper {

    @RedisCache(prefix = "user:base", key = "#id", expire = 3000, returnType = UserBase.class,
                operation = DataOperationType.SELECT, readStrategy = ReadStrategyType.LAZY_LOAD)
    UserBase selectById(@Param("id") Long id);

    UserBase selectByUsername(@Param("username") String username);

    UserBase selectByEmail(@Param("email") String email);

    int updateLastLoginTime(@Param("userId") Long userId, @Param("lastLoginTime") LocalDateTime lastLoginTime);

    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);

    void updateById(UserBase userBase);
    
    void insert(UserBase userBase);

    Long countUsers();
    
    // 检查用户名是否存在
    int countByUsername(@Param("username") String username);
    
    // 检查邮箱是否存在
    int countByEmail(@Param("email") String email);

    // 批量查询
    List<UserBase> selectByIds(@Param("ids") List<Long> ids);
    
    // 查询所有用户（用于管理操作）
    List<UserBase> selectAll();
}
