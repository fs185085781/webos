package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;
import lombok.Data;

/**
 * io_file_ass
 */
@Data
public class IoFileAss extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 用户软件id
     */
    private String softUserId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 拓展名
     */
    private String ext;
    /**
     * 行为addall,openwith,new
     */
    private String action;
    /**
     * 操作名称
     */
    private String actionName;
    /**
     * 注册的按钮A-Z大写
     */
    private String bindKey;
    /**
     * 排序越大越靠前
     */
    private long sortNum;
    /**
     * 地址
     */
    private String url;
    /**
     * 图标地址
     */
    private String iconUrl;
    /**
     * 拓展动作
     */
    private String expAction;
    /**
     * 软件名称
     */
    private String appName;

}
