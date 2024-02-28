package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;
import lombok.Data;

/**
 * sys_dict_detail
 */
@Data
public class SysDictDetail extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 字典id
     */
    private String code;
    /**
     * 字典项的值
     */
    private String val;
    /**
     * 字典项中文
     */
    private String name;
    /**
     * 拓展功能
     */
    private String expand;

}
