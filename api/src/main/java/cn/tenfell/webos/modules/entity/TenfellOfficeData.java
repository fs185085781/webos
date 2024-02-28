package cn.tenfell.webos.modules.entity;

import cn.tenfell.webos.common.bean.CommonBean.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * tenfell_office_data
 * 暂不考虑角色
 */
@Data
public class TenfellOfficeData extends BaseEntity {
    /**
     * 主键id
     */
    private String id;

}
