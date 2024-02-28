package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;
import lombok.Data;

/**
 * sys_config
 */
@Data
public class SysConfig extends BaseEntity {

    /**
     * 主键id
     */
    private String id;
    /**
     * 父级用户Id
     */
    private String parentUserId;
    /**
     * 是否开放注册0关闭1开放
     */
    private Integer openReg;

}
