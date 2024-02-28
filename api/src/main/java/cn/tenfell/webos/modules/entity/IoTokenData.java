package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * io_token_data
 * 硬盘token数据
 */
@Data
public class IoTokenData extends CommonBean.BaseEntity {
    /**
     * 主键id
     * 固定算法  md5(driveType + 网址 + 用户id)
     */
    private String id;
    /**
     * 硬盘类型
     */
    private String driveType;
    /**
     * token数据
     */
    private String tokenData;
    /**
     * 需要在这之前提前20分钟刷新token
     */
    private LocalDateTime expireTime;

    /**
     * 与token无关的拓展数据
     * 方便拓展备用
     */
    private String expData;
    /**
     * 错误次数
     */
    private Integer errCount;
}
