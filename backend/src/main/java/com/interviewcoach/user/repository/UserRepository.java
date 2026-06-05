package com.interviewcoach.user.repository;

import com.interviewcoach.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户数据访问接口。
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * 根据用户名查找用户。
     *
     * @param username 用户名
     * @return 用户实体，不存在时为空
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据 Apple 用户标识查找用户。
     *
     * @param appleUserId Apple 用户唯一标识（sub）
     * @return 用户实体，不存在时为空
     */
    Optional<User> findByAppleUserId(String appleUserId);
}
