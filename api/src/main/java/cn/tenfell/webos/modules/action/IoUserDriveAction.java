package cn.tenfell.webos.modules.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.tenfell.webos.common.annt.Action;
import cn.tenfell.webos.common.annt.Auth;
import cn.tenfell.webos.common.annt.BeanAction;
import cn.tenfell.webos.common.annt.Transactional;
import cn.tenfell.webos.common.bean.CommonBean;
import cn.tenfell.webos.common.filesystem.FileSystemInface;
import cn.tenfell.webos.common.filesystem.FileSystemUtil;
import cn.tenfell.webos.common.server.R;
import cn.tenfell.webos.common.util.DbUtil;
import cn.tenfell.webos.common.util.LoginAuthUtil;
import cn.tenfell.webos.common.util.ValidaUtil;
import cn.tenfell.webos.modules.entity.IoDrive;
import cn.tenfell.webos.modules.entity.IoUserDrive;
import cn.tenfell.webos.modules.entity.IoUserStar;
import cn.tenfell.webos.modules.entity.SysUser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@BeanAction(val = "ioUserDrive")
public class IoUserDriveAction {
    @Action(val = "starList", type = 1)
    public R starList(Dict param) {
        SysUser user = LoginAuthUtil.getUser();
        List<IoUserStar> list = DbUtil.queryList("select * from io_user_star where user_id = ?", IoUserStar.class, user.getId());
        return R.okData(list);
    }

    /**
     * 获取特殊目录
     */
    @Action(val = "specialPath", type = 1)
    public R specialPath(Dict param) {
        String type = param.getStr("type");
        Assert.notBlank(type, "参数错误");
        SysUser user = LoginAuthUtil.getUser();
        IoUserStar ius = DbUtil.queryObject("select * from io_user_star where user_id = ? and type = ?", IoUserStar.class, user.getId(), type);
        Assert.notNull(ius, "当前用户暂无此目录");
        return R.okData(ius.getPath());
    }

    /**
     * 获取特殊目录文件
     */
    @Action(val = "specialFiles", type = 1)
    public R specialFiles(Dict param) {
        String type = param.getStr("type");
        Assert.notBlank(type, "参数错误");
        SysUser user = LoginAuthUtil.getUser();
        IoUserStar ius = DbUtil.queryObject("select * from io_user_star where user_id = ? and type = ?", IoUserStar.class, user.getId(), type);
        Assert.notNull(ius, "当前用户暂无此目录");
        FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPathByLogin(ius.getPath(), "", "");
        String next = "";
        List<CommonBean.Page<CommonBean.PathInfo>> pages = new ArrayList<>();
        while (true) {
            CommonBean.Page<CommonBean.PathInfo> page = FileSystemUtil.ACTION.listFiles(plainPath, next);
            pages.add(page);
            if (page.getType() != 0 && StrUtil.isNotBlank(page.getNext())) {
                next = page.getNext();
            } else {
                break;
            }
        }
        List<CommonBean.PathInfo> list = new ArrayList<>();
        for (CommonBean.Page<CommonBean.PathInfo> page : pages) {
            if (page == null) {
                continue;
            }
            List<CommonBean.PathInfo> tmps = page.getList();
            if (tmps == null || tmps.size() == 0) {
                continue;
            }
            list.addAll(tmps);
        }
        for (CommonBean.PathInfo info : list) {
            info.setPath(ius.getPath() + "/" + info.getPath());
        }
        return R.ok(Dict.create().set("list", list).set("parentPath", ius.getPath()), "获取成功");
    }
    public static CommonBean.PageRes<IoUserDrive> userDriveList(Dict param){
        SysUser user = LoginAuthUtil.getUser();
        String where = " where 1=1";
        List<Object> params = new ArrayList<>();
        String selectData = "select *";
        if (!LoginAuthUtil.isSystem()) {
            boolean isMain = LoginAuthUtil.isMain();
            if (isMain) {
                //主用户
                selectData = "select *";
                where += " and parent_user_id = ?";
                params.add(user.getId());
            } else {
                //子用户
                selectData = "select id,name,use_size,max_size,avail_size,no,is_system";
                where += " and valid = 1 and user_id = ?";
                params.add(user.getId());
            }
        }
        if (StrUtil.isNotBlank(param.getStr("userId"))) {
            where += " and user_id = ?";
            params.add(param.getStr("userId"));
        }
        int currentPage = param.getInt("current");
        Integer pageSize = param.getInt("pageSize");
        CommonBean.PageRes<IoUserDrive> data = DbUtil.pageObject(selectData, "from io_user_drive" + where, IoUserDrive.class, currentPage, pageSize, params.toArray());
        if (LoginAuthUtil.isMain()) {
            boolean isAdmin = LoginAuthUtil.isSystem();
            for (IoUserDrive iud : data.getData()) {
                String name = iud.getName();
                if (isAdmin) {
                    name = "(" + UserAction.getUserName(iud.getParentUserId()) + "-" + UserAction.getUserName(iud.getUserId()) + ")" + name;
                } else {
                    name = "(" + UserAction.getUserName(iud.getUserId()) + ")" + name;
                }
                iud.setName(name);
            }
        }
        return data;
    }

    /**
     * 获取盘列表
     */
    @Action(val = "list", type = 1)
    public R list(Dict param) {
        return R.ok(userDriveList(param), "获取成功");
    }

    @Action(val = "info", type = 1)
    public R info(IoUserDrive data) {
        SysUser user = LoginAuthUtil.getUser();
        String where = " where id = ?";
        List<Object> params = new ArrayList<>();
        params.add(data.getId());
        if (LoginAuthUtil.isMain()) {
            //主用户
            where += " and parent_user_id = ?";
            params.add(user.getId());
        } else {
            //子用户
            where += " and valid = 1 and user_id = ?";
            params.add(user.getId());
        }
        IoUserDrive db = DbUtil.queryObject("select * from io_user_drive" + where, IoUserDrive.class, params.toArray());
        Assert.notNull(db, "当前记录不存在");
        return R.ok(db, "获取成功");
    }

    public static void saveIoUserDrive(IoUserDrive param, IoDrive id, SysUser user) {
        String currentPath = "{io:" + id.getNo() + "}";
        FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPath(currentPath);
        String fileId = FileSystemUtil.ACTION.createDir(plainPath, IdUtil.fastSimpleUUID());
        Assert.notBlank(fileId, "目录创建失败");
        IoUserDrive data = new IoUserDrive();
        SysUser parentUser = DbUtil.queryObject("select * from sys_user where id = ?", SysUser.class, id.getParentUserId());
        Assert.notNull(parentUser, "父级用户不可为空");
        Assert.isTrue(StrUtil.equals(parentUser.getParentUserNo(), user.getParentUserNo()), "此盘不可分配给此用户");
        data.setParentUserId(parentUser.getId());
        data.setUserId(user.getId());
        data.setDriveId(param.getDriveId());
        if (StrUtil.isNotBlank(param.getName())) {
            data.setName(param.getName());
        } else {
            data.setName(id.getUserDriveName());
        }
        data.setPath(currentPath + "/" + fileId);
        data.setUseSize(0L);
        data.setMaxSize(param.getMaxSize());
        data.setAvailSize(data.getMaxSize());
        data.setCreatedTime(LocalDateTime.now());
        data.setUpdatedTime(data.getCreatedTime());
        id.setUseSize(id.getUseSize() + data.getMaxSize());
        long ava = id.getMaxSize() - id.getUseSize();
        Assert.isTrue(ava >= 0, "当前硬盘可分配容量不足");
        id.setAvailSize(ava);
        boolean flag = DbUtil.updateObject(id, Entity.create().set("id", id.getId())) > 0;
        Assert.isTrue(flag, "更新硬盘出错");
        long no = DbUtil.queryLong("select max(`no`) as `max_no` from `io_user_drive`", IoUserDrive.class);
        no++;
        data.setNo((int) no);
        data.setValid(param.getValid());
        data.setId(IdUtil.fastSimpleUUID());
        DbUtil.insertObject(data);
        int isSystem = checkAndCreateUserStar(user.getId(), "{uio:" + no + "}", data.getId());
        data.setIsSystem(isSystem);
        DbUtil.upsertObject(data, "id");
    }

    /**
     * 添加用户磁盘
     * 主用户专享
     */
    @Action(val = "save", type = 1)
    @Auth(val = "main_auth")
    @Transactional
    public R save(IoUserDrive param) {
        //userId driveId maxSize name valid
        ValidaUtil.init(param)
                .notBlank("userId", "用户选择")
                .notBlank("driveId", "磁盘选择")
                .greater("maxSize", 0, "最大大小")
                .notBlank("name", "磁盘名称")
                .notBlank("valid", "启用状态");
        SysUser loginUser = LoginAuthUtil.getUser();
        IoDrive id = DbUtil.queryObject("select * from io_drive where id = ?", IoDrive.class, param.getDriveId());
        Assert.notNull(id, "此硬盘不存在");
        String sql = "select * from sys_user where id = ?";
        List<Object> params = new ArrayList<>();
        params.add(param.getUserId());
        if (!LoginAuthUtil.isSystem()) {
            //非管理员主用户,只能操作自己和自己用户下的
            sql += " and parent_user_no = ?";
            params.add(loginUser.getParentUserNo());
            Assert.isTrue(StrUtil.equals(id.getParentUserId(), loginUser.getId()), "此硬盘不存在");
        }
        SysUser user = DbUtil.queryObject(sql, SysUser.class, params.toArray());
        Assert.notNull(user, "此用户不存在");
        saveIoUserDrive(param, id, user);
        return R.ok();
    }

    private static int checkAndCreateUserStar(String userId, String parentPath, String iudId) {
        List<Dict> list = CollUtil.newArrayList(
                Dict.create().set("name", "桌面").set("type", "desktop"),
                Dict.create().set("name", "下载").set("type", "downloads"),
                Dict.create().set("name", "文档").set("type", "documents"),
                Dict.create().set("name", "视频").set("type", "videos"),
                Dict.create().set("name", "图片").set("type", "pictures"),
                Dict.create().set("name", "音乐").set("type", "music")
        );
        int isSystem = 2;
        for (int i = 0; i < list.size(); i++) {
            Dict item = list.get(i);
            long count = DbUtil.queryLong("select count(0) from io_user_star where user_id = ? and type = ?", IoUserStar.class, userId, item.getStr("type"));
            if (count == 0) {
                IoUserStar ius = new IoUserStar();
                ius.setIudId(iudId);
                ius.setId(IdUtil.fastSimpleUUID());
                ius.setUserId(userId);
                ius.setName(item.getStr("name"));
                FileSystemInface.PlainPath plainPath = FileSystemUtil.cipherPath2PlainPath(parentPath);
                String fileId = FileSystemUtil.ACTION.createDir(plainPath, item.getStr("name"));
                Assert.notBlank(fileId, item.getStr("name") + "目录创建失败");
                ius.setPath(parentPath + "/" + fileId);
                ius.setType(item.getStr("type"));
                boolean flag = DbUtil.insertObject(ius);
                Assert.isTrue(flag, item.getStr("name") + "目录保存失败");
                if (StrUtil.equals(item.getStr("type"), "desktop")) {
                    isSystem = 1;
                }
            }
        }
        return isSystem;
    }

    /**
     * 修改硬盘
     * 主用户专享
     */
    @Action(val = "update", type = 1)
    @Auth(val = "main_auth")
    @Transactional
    public R update(IoUserDrive param) {
        //id,userId,maxSize name valid
        SysUser loginUser = LoginAuthUtil.getUser();
        IoUserDrive dbData = DbUtil.queryObject("select * from io_user_drive where id = ? and parent_user_id = ?", IoUserDrive.class, param.getId(), loginUser.getId());
        Assert.notNull(dbData, "此网盘不存在");
        if (StrUtil.isNotBlank(param.getUserId()) && !StrUtil.equals(param.getUserId(), dbData.getUserId())) {
            SysUser user = DbUtil.queryObject("select * from sys_user where id = ? and parent_user_no = ?", SysUser.class, param.getUserId(), loginUser.getParentUserNo());
            Assert.notNull(user, "此用户不存在");
            dbData.setUserId(user.getId());
        }
        if (param.getMaxSize() > 0 && param.getMaxSize() != dbData.getMaxSize()) {
            IoDrive id = DbUtil.queryObject("select * from io_drive where id = ? and parent_user_id = ?", IoDrive.class, dbData.getDriveId(), loginUser.getId());
            Assert.notNull(id, "此硬盘不存在");
            id.setUseSize(id.getUseSize() - dbData.getMaxSize() + param.getMaxSize());
            long ava = id.getMaxSize() - id.getUseSize();
            Assert.isTrue(ava >= 0, "当前硬盘可分配容量不足");
            id.setAvailSize(ava);
            boolean flag = DbUtil.updateObject(id, Entity.create().set("id", id.getId())) > 0;
            Assert.isTrue(flag, "更新硬盘出错");
            dbData.setMaxSize(param.getMaxSize());
        }
        if (StrUtil.isNotBlank(param.getName()) && !StrUtil.equals(param.getName(), dbData.getName())) {
            dbData.setName(param.getName());
        }
        dbData.setValid(param.getValid());
        dbData.setUpdatedTime(LocalDateTime.now());
        return DbUtil.commonEdit(dbData);
    }
    public static void delIoUserDriveByIds(List<IoUserDrive> list){
        if(CollUtil.isEmpty(list)){
            return;
        }
        List<String> iudIds = CollUtil.getFieldValues(list, "id", String.class);
        String iudIdsStr = StrUtil.format("'{}'", CollUtil.join(iudIds, "','"));
        DbUtil.delete("delete from io_user_drive where id in ( " + iudIdsStr + " )", IoUserDrive.class);
        DbUtil.delete("delete from io_user_star where iud_id in ( " + iudIdsStr + " )", IoUserStar.class);
    }

    @Action(val = "dels", type = 1)
    @Auth(val = "main_auth")
    @Transactional
    public R dels(List<String> ids) {
        Assert.notEmpty(ids, "当前数据不存在");
        SysUser user = LoginAuthUtil.getUser();
        String idsStr = StrUtil.format("'{}'", CollUtil.join(ids, "','"));
        String where = "";
        List<Object> params = new ArrayList<>();
        if (!LoginAuthUtil.isSystem()) {
            where += " and parent_user_id = ?";
            params.add(user.getId());
        }
        List<IoUserDrive> list = DbUtil.queryList("select * from io_user_drive where id in ( " + idsStr + " )" + where, IoUserDrive.class, params.toArray());
        delIoUserDriveByIds(list);
        return R.ok();
    }
}
