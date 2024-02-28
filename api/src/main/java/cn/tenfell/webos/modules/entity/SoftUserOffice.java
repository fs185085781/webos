package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * soft_user_office
 */
@Data
public class SoftUserOffice extends CommonBean.BaseEntity {
    /**
     * 主键id
     * 由真实路径md5生成
     */
    private String id;
    /**
     * 绑定时候的路径
     */
    private String path;
    /**
     * 父级目录
     */
    private String parentPath;
    /**
     * 文件名
     */
    private String name;
    /**
     * 绑定的用户id
     */
    private String userId;
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    /**
     * 金山文档的id
     */
    private String jinShanId;
    /**
     * 团队id
     */
    private String groupId;
}
