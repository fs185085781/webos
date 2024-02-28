package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;
import lombok.Data;

/**
 * io_offline_download
 */
@Data
public class IoOfflineDownload extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 主用户id
     */
    private String parentUserId;
    /**
     * 下载地址
     */
    private String downUrl;
    /**
     * 0已创建1下载中2下载完成3下载失败
     */
    private long status;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 当前文件的位置相对于user_drive的位置,如{io:1}/abc
     */
    private String parentPath;
    /**
     * 文件名
     */
    private String name;

}
