package cn.tenfell.webos.modules.entity;

import lombok.Data;

/**
 * soft_user
 */
@Data
public class SoftUser {
    private String id;
    /**
     * 图标地址
     */
    private String imgPath;
    /**
     * 名称
     */
    private String name;
    /**
     * 编码(轻应用编码为md5的iframeUrl)
     */
    private String code;
    /**
     * 描述
     */
    private String descr;
    /**
     * 截图json
     */
    private String screenShots;
    /**
     * 版本号
     */
    private String version;
    /**
     * 作者
     */
    private String author;
    /**
     * 功能描述
     */
    private String effect;
    /**
     * 0插件1轻应用
     */
    private Integer type;
    /**
     * 轻应用地址
     */
    private String iframeUrl;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 商城id
     */
    private String storeId;
    /**
     * 下载地址
     */
    private String downloadUrl;
    /**
     * 1商城app 0本地app
     */
    private Integer isStore;
}
