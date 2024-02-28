package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;
import lombok.Data;

/**
 * sys_themes
 */
@Data
public class SysThemes extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * null
     */
    private String name;
    /**
     * null
     */
    private long backgroundUrl;
    /**
     * 0网络主题1自定义主题
     */
    private long type;
    /**
     * type=1时必填
     */
    private String userId;
    /**
     * 样式表下载地址
     */
    private String cssDownUrl;
    /**
     * 和background_url只能二选一
     */
    private String backgroundColor;
    /**
     * macos,deepin,win11,win10,mobile单选
     */
    private String supportSys;
    /**
     * 0非官方 1官方
     */
    private long officialTheme;
    /**
     * 软件状态0未提交1审核中2已上架3已打回
     */
    private long officialStatus;
    /**
     * 更新日志
     */
    private String updateLog;
    /**
     * 主题主页
     */
    private String homePages;
    /**
     * 作者
     */
    private String author;
    /**
     * 远程商城主题id
     */
    private long cloudThemeId;
    /**
     * 分类编码
     */
    private String catCode;
    /**
     * 主题编码/目录
     */
    private String themeCode;

}
