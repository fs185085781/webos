package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * soft_apps
 */
@Data
public class SoftApps extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 软件编码/软件目录
     */
    private String appCode;
    /**
     * 软件名称
     */
    private String name;
    /**
     * 分类编码
     */
    private String catCode;
    /**
     * 当前版本号
     */
    private String currentVersion;
    /**
     * 最新版本号
     */
    private String lastVersion;
    /**
     * 软件主页
     */
    private String homePages;
    /**
     * 作者名称
     */
    private String author;
    /**
     * 最后从官方拉取的时间
     */
    private LocalDateTime lastUpdatedTime;
    /**
     * 0非官方软件1官方软件
     */
    private long officialSoft;
    /**
     * 软件状态0未提交1审核中2已上架3已打回
     */
    private long officialStatus;
    /**
     * 图标https地址
     */
    private String iconUrl;
    /**
     * 0插件1轻应用
     */
    private long type;
    /**
     * type=1才有效,轻应用地址
     */
    private String iframeUrl;
    /**
     * type=0才有效,软件包下载地址
     */
    private String downUrl;
    /**
     * 更新日志
     */
    private String updateLog;
    /**
     * 支持系统lite,macos,deepin,win11,win10,mobile,英文逗号分割
     */
    private String supportSys;
    /**
     * 支持的版本和support_sys是同步的,逗号分割,均支持请用0代替
     */
    private String supportVersion;
    /**
     * official_soft为0时必填
     */
    private String userId;
    /**
     * 远程商城的app_id
     */
    private String cloudAppId;
    /**
     * 相对插件目录地址
     */
    private String settingPage;

}
