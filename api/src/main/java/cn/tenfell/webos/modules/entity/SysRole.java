package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * sys_role
 * 暂不考虑角色
 */
@Data
public class SysRole extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 角色名称
     */
    private String name;
    /**
     * 权限编码
     */
    private String authKey;
    /**
     * null
     */
    private LocalDateTime createdTime;
    /**
     * null
     */
    private LocalDateTime updatedTime;
    /**
     * 主用户id
     */
    private String parentUserId;

}
