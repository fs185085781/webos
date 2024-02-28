package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;
import lombok.Data;

/**
 * sys_role_menu
 * 暂不考虑角色
 */
@Data
public class SysRoleMenu extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 主用户id
     */
    private String parentUserId;
    /**
     * null
     */
    private String roleId;
    /**
     * null
     */
    private String menuId;

}
