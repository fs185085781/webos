package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;

import java.util.List;

import lombok.Data;

/**
 * sys_dict
 */
@Data
public class SysDict extends BaseEntity {

    /**
     * null
     */
    private String id;
    /**
     * 字典编码
     */
    private String code;
    /**
     * 字典中文名
     */
    private String name;
    /**
     * 字典中文描述
     */
    private String descr;

    /**
     * 非数据库字段子数据
     */
    private List<SysDictDetail> details;

}
