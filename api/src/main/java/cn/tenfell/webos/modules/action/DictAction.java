package cn.tenfell.webos.modules.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.tenfell.webos.common.annt.*;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.DbUtil;
import cn.tenfell.webos.modules.entity.SysDict;
import cn.tenfell.webos.modules.entity.SysDictDetail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@BeanAction(val = "dict")
public class DictAction {
    /**
     * 获取字典列表
     * 主用户专享
     */
    @Action(val = "list", type = 1)
    @Auth(val = "system")
    public R list(Dict param) {
        int currentPage = param.getInt("current");
        Integer pageSize = param.getInt("pageSize");
        CommonBean.PageRes<SysDict> data = DbUtil.pageObject("select *", "from sys_dict", SysDict.class, currentPage, pageSize);
        if (CollUtil.isNotEmpty(data.getData())) {
            String codesStr = StrUtil.format("'{}'", CollUtil.join(CollUtil.getFieldValues(data.getData(), "code", String.class), "','"));
            List<SysDictDetail> list = DbUtil.queryList("select * from sys_dict_detail where code in ( " + codesStr + " )", SysDictDetail.class);
            if (CollUtil.isNotEmpty(list)) {
                Map<String, List<SysDictDetail>> map = new HashMap<>();
                List<List<SysDictDetail>> tmps = CollUtil.groupByField(list, "code");
                for (List<SysDictDetail> tmp : tmps) {
                    map.put(tmp.get(0).getCode(), tmp);
                }
                data.getData().forEach(sysDict -> sysDict.setDetails(map.get(sysDict.getCode())));
            }
        }
        return R.ok(data, "获取成功");
    }

    /**
     * 新增编辑字典
     * 主用户专享
     */
    @Action(val = "edit", type = 1)
    @Auth(val = "system")
    public R edit(SysDict data) {
        return DbUtil.commonEdit(data);
    }

    /**
     * 新增编辑字典项
     * 主用户专享
     */
    @Action(val = "childEdit", type = 1)
    @Auth(val = "system")
    public R childEdit(SysDictDetail data) {
        return DbUtil.commonEdit(data);
    }

    /**
     * 获取单个字典
     * 主用户专享
     */
    @Action(val = "info", type = 1)
    @Auth(val = "system")
    public R info(SysDict data) {
        SysDict db = DbUtil.queryObject("select * from sys_dict where id = ?", SysDict.class, data.getId());
        Assert.notNull(db, "当前记录不存在");
        return R.ok(db, "获取成功");
    }

    /**
     * 获取单个字典项
     * 主用户专享
     */
    @Action(val = "childInfo", type = 1)
    @Auth(val = "system")
    public R childInfo(SysDictDetail data) {
        SysDictDetail db = DbUtil.queryObject("select * from sys_dict_detail where id = ?", SysDictDetail.class, data.getId());
        Assert.notNull(db, "当前记录不存在");
        return R.ok(db, "获取成功");
    }

    /**
     * 删除字典
     * 主用户专享
     */
    @Action(val = "dels", type = 1)
    @Auth(val = "system")
    @Transactional
    public R dels(List<String> codes) {
        Assert.notEmpty(codes, "当前数据不存在");
        String codesStr = StrUtil.format("'{}'", CollUtil.join(codes, "','"));
        int count = DbUtil.delete("delete from sys_dict where code in ( " + codesStr + " )", SysDict.class);
        DbUtil.delete("delete from sys_dict_detail where code in ( " + codesStr + " )", SysDictDetail.class);
        Assert.isTrue(count > 0, "删除失败");
        return R.ok(null, "删除成功");
    }

    /**
     * 删除字典项
     * 主用户专享
     */
    @Action(val = "childDels", type = 1)
    @Auth(val = "system")
    public R childDels(List<String> ids) {
        Assert.notEmpty(ids, "当前数据不存在");
        String idsStr = StrUtil.format("'{}'", CollUtil.join(ids, "','"));
        int count = DbUtil.delete("delete from sys_dict_detail where id in ( " + idsStr + " )", SysDictDetail.class);
        Assert.isTrue(count > 0, "删除失败");
        return R.ok(null, "删除成功");
    }

    /**
     * 获取下拉框
     */
    @Action(val = "select", type = 1)
    @Login(val = false)
    public R select(SysDict dict) {
        List<SysDictDetail> list = selectData(dict);
        return R.ok(list, "获取成功");
    }

    private List<SysDictDetail> selectData(SysDict dict) {
        return DbUtil.queryList("select * from sys_dict_detail where code = ?", SysDictDetail.class, dict.getCode());
    }

    /**
     * 获取下拉框Map
     */
    @Action(val = "selectMap", type = 1)
    @Login(val = false)
    public R selectMap(SysDict dict) {
        List<SysDictDetail> list = selectData(dict);
        Map<String, String> data = CollUtil.fieldValueAsMap(list, "val", "name");
        return R.ok(data, "获取成功");
    }


}
