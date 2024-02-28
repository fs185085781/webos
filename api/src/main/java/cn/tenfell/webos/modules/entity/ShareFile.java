package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * share_file
 */
@Data
public class ShareFile extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 分享编码
     */
    private String code;
    /**
     * 分享人
     */
    private String userId;
    /**
     * 浏览次数
     */
    private int viewNum;
    /**
     * 下载次数
     */
    private int downNum;
    /**
     * 分享时间
     */
    private LocalDateTime shareTime;
    /**
     * 密码
     */
    private String password;
    /**
     * 到期时间
     */
    private LocalDate expireTime;
    /**
     * 以{io:0}或者{uio:0}开头
     */
    private String path;
    /**
     * sio的编号
     */
    private int no;
    /**
     * 多个分号分割
     */
    private String files;
    /**
     * 名称
     */
    private String name;
}
