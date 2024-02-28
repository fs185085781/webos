package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * sys_user
 */
@Data
public class SysUser extends BaseEntity {

    /**
     * 主键id
     */
    private String id;
    /**
     * 父级用户编号
     */
    private String parentUserNo;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 文件路径
     */
    private String imgPath;
    /**
     * 昵称
     */
    private String nickName;
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
    /**
     * 当前主题
     */
    private String themeId;
    /**
     * 锁屏密码
     */
    private String spPassword;
    /**
     * 用户类型1主用户2子用户
     */
    private Integer userType;
    /**
     * 是否有效
     * 1有效(启用)
     * 2无效(禁用)
     */
    private Integer valid;
    /**
     * 是否管理员
     * 1是
     * 2否
     */
    private Integer isAdmin;

}
