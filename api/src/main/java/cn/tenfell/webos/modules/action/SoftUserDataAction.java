package cn.tenfell.webos.modules.action;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.db.Entity;
import cn.tenfell.webos.common.annt.Action;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.DbUtil;
import cn.tenfell.webos.common.util.LoginAuthUtil;
import cn.tenfell.webos.modules.entity.SoftUserData;
import cn.tenfell.webos.modules.entity.SysUser;

@BeanAction(val = "softUserData")
public class SoftUserDataAction {
    /**
     * 保存数据
     */
    @Action(val = "save", type = 1)
    public R save(SoftUserData param) {
        Assert.notBlank(param.getAppCode(), "软件编码不可为空");
        SysUser user = LoginAuthUtil.getUser();
        SoftUserData dbData = DbUtil.queryObject("select * from soft_user_data where user_id = ? and app_code = ?", SoftUserData.class, user.getId(), param.getAppCode());
        if (dbData == null) {
            dbData = new SoftUserData();
            dbData.setUserId(user.getId());
            dbData.setAppCode(param.getAppCode());
            dbData.setData(param.getData());
            dbData.setId(IdUtil.fastSimpleUUID());
            Assert.isTrue(DbUtil.insertObject(dbData), "保存失败");
        } else {
            dbData.setData(param.getData());
            Assert.isTrue(DbUtil.updateObject(dbData, Entity.create().set("id", dbData.getId())) > 0, "保存失败");
        }
        return R.ok();
    }

    @Action(val = "get", type = 1)
    public R get(SoftUserData param) {
        Assert.notBlank(param.getAppCode(), "软件编码不可为空");
        SysUser user = LoginAuthUtil.getUser();
        SoftUserData dbData = DbUtil.queryObject("select * from soft_user_data where user_id = ? and app_code = ?", SoftUserData.class, user.getId(), param.getAppCode());
        if (dbData == null) {
            return R.failed();
        }
        return R.okData(dbData.getData());
    }
}
