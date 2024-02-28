package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;
import lombok.Data;

/**
 * sys_menu
 */
@Data
public class SysMenu extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 功能名
     */
    private String name;
    /**
     * 类型0-9功能10行为0设置
     */
    private long type;
    /**
     * 主用户id
     */
    private String parentUserId;
    /**
     * null
     */
    private String authKey;

}
