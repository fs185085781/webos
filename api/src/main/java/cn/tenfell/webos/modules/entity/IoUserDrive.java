package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * io_user_drive
 */
@Data
public class IoUserDrive extends BaseEntity {

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
     * 硬盘id
     */
    private String driveId;
    /**
     * 磁盘名称
     */
    private String name;
    /**
     * 相对于drive的位置,此位置之后为该用户可操作的位置,例如
     * {io:1}/abc
     * 如果此时文件路径为{uio:1}/ccf.txt
     * {io:1}的path为/test
     * 则等价于实际的文件在/test/abc/ccf.txt
     */
    private String path;
    /**
     * 已用大小MB
     */
    private long useSize;
    /**
     * 最大大小MB,不能为0
     */
    private long maxSize;
    /**
     * 可用大小MB,不可为0
     */
    private long availSize;
    /**
     * null
     */
    private LocalDateTime createdTime;
    /**
     * null
     */
    private LocalDateTime updatedTime;

    /**
     * 磁盘编号 从1开始
     * 文件会以{uio:1}开头
     */
    private Integer no;

    /**
     * 1有效 2无效
     */
    private Integer valid;
    /**
     * 是否系统盘 1是 2否
     */
    private Integer isSystem;

}
