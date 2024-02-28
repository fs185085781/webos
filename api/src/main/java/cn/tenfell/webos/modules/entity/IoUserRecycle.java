package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * io_user_recycle
 */
@Data
public class IoUserRecycle extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * null
     */
    private String userId;
    /**
     * 大小
     */
    private long size;
    /**
     * 类型
     */
    private int type;
    /**
     * 文件名/目录名
     */
    private String name;
    /**
     * 删除时间
     */
    private LocalDateTime deletedTime;
    /**
     * 原删除路径
     * 第三方盘目录:{uio:1}/dirId/dirId
     * 第三方盘文件:{uio:1}/dirId/fileId
     * 本地盘目录:{uio:1}/一级目录/二级目录
     * 本地盘文件:{uio:1}/一级目录/张三.mp3
     */
    private String removePath;

}
