package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * io_user_star
 */
@Data
public class IoUserStar extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 显示名称
     */
    private String name;
    /**
     * {uio开头}
     */
    private String path;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 类型
     * download 下载
     * desktop 桌面
     * documents 文档
     * star 收藏夹
     */
    private String type;

    private String iudId;
}
