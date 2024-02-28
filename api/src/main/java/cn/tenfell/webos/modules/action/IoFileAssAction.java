package cn.tenfell.webos.modules.action;

import cn.hutool.core.lang.Dict;
import cn.tenfell.webos.common.annt.Action;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.DbUtil;
import cn.tenfell.webos.common.util.LoginAuthUtil;
import cn.tenfell.webos.modules.entity.IoFileAss;
import cn.tenfell.webos.modules.entity.SysUser;

import java.util.List;

@BeanAction(val = "ioFileAss")
public class IoFileAssAction {
    @Action(val = "list", type = 1)
    public R list(Dict param) {
        SysUser user = LoginAuthUtil.getUser();
        List<IoFileAss> list = DbUtil.queryList("select * from io_file_ass where user_id = ?", IoFileAss.class, user.getId());
        return R.ok(list, "获取成功");
    }
}
