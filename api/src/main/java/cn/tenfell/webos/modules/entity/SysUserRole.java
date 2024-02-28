package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;
import lombok.Data;

/**
 * sys_user_role
 * 暂不考虑角色
 */
@Data
public class SysUserRole extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 主用户或子用户id
     */
    private String userId;
    /**
     * null
     */
    private String roleId;
    /**
     * 主用户id
     */
    private String parentUserId;

}
