package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * sys_theme_user
 */
@Data
public class SysThemeUser extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 主用户id
     */
    private String parentUserId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 商城主题id
     */
    private String themeId;
    /**
     * null
     */
    private LocalDateTime createdTime;
    /**
     * null
     */
    private LocalDateTime updatedTime;

}
