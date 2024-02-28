package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * sys_log
 */
@Data
public class SysLog extends BaseEntity {

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
     * 操作内容
     */
    private String action;
    /**
     * 描述
     */
    private String descr;
    /**
     * null
     */
    private LocalDateTime actionTime;

}
