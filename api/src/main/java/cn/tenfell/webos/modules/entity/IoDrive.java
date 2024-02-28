package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * io_drive
 */
@Data
public class IoDrive extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 硬盘名称
     */
    private String name;
    /**
     * 物理路径,此路径之后的位置为用户可操作的位置,例如:
     * 本地文件 /test/aaa
     * 本地文件 D:\iodrive_test
     * 阿里云盘 62c803562d0bd15e70cd409b97e9b4195526f180
     * 123云盘 1657351
     */
    private String path;
    /**
     * 已用大小MB,等价于所有user_drive的max_size和
     */
    private long useSize;
    /**
     * 最大大小MB,0表示无限制
     */
    private long maxSize;
    /**
     * 可用大小MB,0表示无限制
     */
    private long availSize;
    /**
     * token表数据
     */
    private String tokenId;
    /**
     * 给用户分配网盘时候的默认盘名
     */
    private String userDriveName;
    /**
     * null
     */
    private LocalDateTime createdTime;
    /**
     * null
     */
    private LocalDateTime updatedTime;
    /**
     * 硬盘类型
     */
    private String driveType;
    /**
     * 主用户id
     */
    private String parentUserId;
    /**
     * 磁盘编号 从1开始
     * 文件会以{io:1}开头
     */
    private Integer no;
    /**
     * 是否支持秒传1支持2不支持
     */
    private Integer secondTransmission;
    /**
     * 秒传下有效,秒传文件真实目录
     */
    private String realFilePath;


}
