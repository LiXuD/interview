package com.interviewcoach.user.repository;

import com.interviewcoach.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
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

    /**
     * 根据微信 openId 查找用户。
     *
     * @param wechatOpenId 微信小程序 openId
     * @return 用户实体，不存在时为空
     */
    Optional<User> findByWechatOpenId(String wechatOpenId);

    /** 统计设置了月度配额的用户数 */
    long countByMonthlyTokenQuotaIsNotNull();

    /** 查询所有设置了月度配额的用户 */
    List<User> findByMonthlyTokenQuotaIsNotNull();

    /** 按用户名模糊搜索 */
    List<User> findByUsernameContainingIgnoreCase(String keyword);

    /** 按用户名或邮箱模糊搜索 */
    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String usernameKeyword, String emailKeyword);

    /** 查询所有用户 ID */
    @Query("SELECT u.id FROM User u")
    List<UUID> findAllIds();
}
