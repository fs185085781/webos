package cn.tenfell.webos.modules.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.tenfell.webos.common.annt.Action;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.annt.Transactional;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.filesystem.FileSystemInface;
import cn.tenfell.webos.common.filesystem.FileSystemUtil;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.DbUtil;
import cn.tenfell.webos.common.util.LoginAuthUtil;
import cn.tenfell.webos.common.util.ProjectUtil;
import cn.tenfell.webos.modules.entity.IoUserRecycle;
import cn.tenfell.webos.modules.entity.SysUser;

import java.util.ArrayList;
import java.util.List;

@BeanAction(val = "userRecycle")
public class UserRecycleAction {
    public static R infoByPath(String path) {
        SysUser user = LoginAuthUtil.getUser();
        if (user == null) {
            return R.failed("请登录后重试");
        }
        String id = path2id(path);
        IoUserRecycle iur = DbUtil.queryObject("select * from io_user_recycle where id = ? and user_id = ?", IoUserRecycle.class, id, user.getId());
        Assert.notNull(iur, "权限不足");
        CommonBean.PathInfo info = new CommonBean.PathInfo();
        info.setPath(iur.getRemovePath());
        info.setType(iur.getType());
        info.setName(iur.getName());
        info.setSize(iur.getSize());
        info.setCreatedAt(LocalDateTimeUtil.formatNormal(iur.getDeletedTime()));
        if (info.getType() == 1) {
            info.setExt(FileUtil.extName(iur.getName()));
        }
        return R.okData(info);
    }

    private static String path2id(String path) {
        String[] sz = path.split(":");
        sz = sz[1].split("}");
        return sz[0];
    }

    public static String id2realpath(String cycleId) {
        return ProjectUtil.rootPath + "/recycle/" + cycleId + ".zip";
    }

    /**
     * 获取回收站列表
     */
    @Action(val = "list", type = 1)
    public R list(Dict param) {
        int currentPage = param.getInt("current");
        Integer pageSize = param.getInt("pageSize");
        String where = " where 1=1";
        List<Object> params = new ArrayList<>();
        where += " and user_id = ?";
        params.add(LoginAuthUtil.getUser().getId());
        CommonBean.PageRes<IoUserRecycle> data = DbUtil.pageObject("select *", "from io_user_recycle" + where, IoUserRecycle.class, currentPage, pageSize, params.toArray());
        return R.ok(data, "获取成功");
    }

    /**
     * 获取回收站列表
     */
    @Action(val = "restoreByPaths", type = 1)
    public R restoreByPaths(List<String> paths) {
        boolean hasError = false;
        boolean hasSuccess = false;
        SysUser user = LoginAuthUtil.getUser();
        for (int i = 0; i < paths.size(); i++) {
            String id = path2id(paths.get(i));
            try {
                IoUserRecycle iur = DbUtil.queryObject("select * from io_user_recycle where id = ? and user_id = ?", IoUserRecycle.class, id, user.getId());
                Assert.notNull(iur, "权限不足");
                FileSystemInface.PlainPath path = FileSystemUtil.cipherPath2PlainPathByLogin(iur.getRemovePath(), "", "");
                boolean flag = FileSystemUtil.ACTION.restore(path, iur);
                if (flag) {
                    hasSuccess = true;
                    DbUtil.delete("delete from io_user_recycle where id = ?", IoUserRecycle.class, id);
                } else {
                    hasError = true;
                }
            } catch (Exception e) {
                hasError = true;
                ProjectUtil.showConsoleErr(e);
            }
        }
        if (hasSuccess && hasError) {
            return R.ok(null, "部分恢复成功");
        } else if (hasSuccess) {
            return R.ok(null, "恢复成功");
        } else {
            return R.failed("恢复失败");
        }
    }

    /**
     * 彻底删除
     */
    @Action(val = "clearByPaths", type = 1)
    @Transactional
    public R clearByPaths(List<String> paths) {
        List<String> ids = new ArrayList<>();
        for (String path : paths) {
            String id = path2id(path);
            ids.add(id);
        }
        delByIds(ids);
        return R.ok();
    }

    private void delByIds(List<String> ids) {
        SysUser user = LoginAuthUtil.getUser();
        String idsStr = "'" + CollUtil.join(ids, "','") + "'";
        int count = DbUtil.delete("delete from io_user_recycle where id in ( " + idsStr + " )  and user_id = ?", IoUserRecycle.class, user.getId());
        if (count == ids.size()) {
            for (String id : ids) {
                String zipPath = id2realpath(id);
                if (FileUtil.exist(zipPath)) {
                    FileUtil.del(zipPath);
                }
            }
        } else {
            Assert.isTrue(false, "彻底删除失败");
        }
    }

    @Action(val = "clear", type = 1)
    @Transactional
    public R clear(Dict param) {
        SysUser user = LoginAuthUtil.getUser();
        List<IoUserRecycle> list = DbUtil.queryList("select id from io_user_recycle where user_id = ?", IoUserRecycle.class, user.getId());
        if (CollUtil.isEmpty(list)) {
            return R.ok();
        }
        List<String> ids = CollUtil.getFieldValues(list, "id", String.class);
        delByIds(ids);
        return R.ok(null, "清空回收站成功");
    }

    @Action(val = "count", type = 1)
    public R count(Dict param) {
        SysUser user = LoginAuthUtil.getUser();
        long count = DbUtil.queryLong("select count(0) from io_user_recycle where user_id = ?", IoUserRecycle.class, user.getId());
        return R.ok(count, "获取数量成功");
    }
}
