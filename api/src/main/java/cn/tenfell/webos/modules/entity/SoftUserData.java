package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * soft_user_data
 */
@Data
public class SoftUserData extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 软件编号
     */
    private String appCode;
    /**
     * 数据
     */
    private String data;
}
